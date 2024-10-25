package com.kalachakra.kalachakra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

@SpringBootApplication
@RestController
public class KalachakraApplication {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	// Rate limit: 10 requests per minute
    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    public static void main(String[] args) {
        SpringApplication.run(KalachakraApplication.class, args);
    }

	@GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

	@GetMapping("/increment")
	public String incrementCount() {
	    String key = "count";
	    Boolean keyExists = redisTemplate.hasKey(key);
	    
	    if (keyExists != null && keyExists) {
	        // Key exists, increment the value
	        Long newValue = redisTemplate.opsForValue().increment(key);
	        return "Key '" + key + "' found. New value after increment: " + newValue;
	    } else {
	        // Key doesn't exist, set it to 1
	        redisTemplate.opsForValue().set(key, "1");
	        return "Key '" + key + "' not found. Set to 1.";
	    }
	}
	
	@GetMapping("/rate-limiter")
	public ResponseEntity<String> rateLimiter(HttpServletRequest request) {
		String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp;

        Long currentRequests = redisTemplate.opsForValue().increment(key);
        
        if (currentRequests != null && currentRequests == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }

        if (currentRequests != null && currentRequests > MAX_REQUESTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }

        // If we haven't exceeded the rate limit, call the protected endpoint
        return ResponseEntity.ok(incrementCount());
	}

	private String getClientIp(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        }
        return xForwardedForHeader.split(",")[0];
    }
}
