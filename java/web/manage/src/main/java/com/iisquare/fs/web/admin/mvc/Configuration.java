package com.iisquare.fs.web.admin.mvc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class Configuration {

    @Value("${custom.date.format}")
    private String dateFormat;

}
