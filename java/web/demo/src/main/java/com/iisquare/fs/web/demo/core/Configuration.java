package com.iisquare.fs.web.demo.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class Configuration {

    @Value("${fs.format.date}")
    private String formatDate;

}
