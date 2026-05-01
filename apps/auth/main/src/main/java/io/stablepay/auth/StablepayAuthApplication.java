package io.stablepay.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StablepayAuthApplication {

  public static void main(String[] args) {
    SpringApplication.run(StablepayAuthApplication.class, args);
  }
}
