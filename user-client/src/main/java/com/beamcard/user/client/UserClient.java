package com.beamcard.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/by-username/{username}")
    UserSummary getByUsername(@PathVariable String username);
}
