/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.Controller;
import play.mvc.Result;
import utils.ObjectMapperConfigUtil;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;

public class HomeController extends Controller {

    private final ObjectMapper mapper;

    @Inject
    public HomeController(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Result index() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-java_injected-mapper.json"), ObjectMapperConfigUtil.toConfigJson(mapper));
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-java_play-libs-mapper.json"), ObjectMapperConfigUtil.toConfigJson(play.libs.Json.mapper()));
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-java_play-libs-newDefaultMapper.json"), ObjectMapperConfigUtil.toConfigJson(play.libs.Json.newDefaultMapper()));
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-java_play-api-libs-json-jackson-JacksonJson-mapper.json"), ObjectMapperConfigUtil.toConfigJson(play.api.libs.json.jackson.JacksonJson$.MODULE$.get().mapper()));
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-java_play-libs-ws-DefaultObjectMapper-instance.json"), ObjectMapperConfigUtil.toConfigJson(play.libs.ws.DefaultObjectMapper.instance()));
        return ok();
    }

}
