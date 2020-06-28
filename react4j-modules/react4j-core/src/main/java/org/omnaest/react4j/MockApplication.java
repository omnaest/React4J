package org.omnaest.react4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Profile("mock")
public class MockApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(MockApplication.class, args);
    }
}
