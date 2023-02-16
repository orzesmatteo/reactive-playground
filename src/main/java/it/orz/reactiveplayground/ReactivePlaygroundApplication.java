package it.orz.reactiveplayground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReactivePlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactivePlaygroundApplication.class, args);
    }

}
