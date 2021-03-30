package com.example.demo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class DemoApplication {
    private RestTemplate restTemplate = new RestTemplate();
    private AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private ObjectMapper mapper = new ObjectMapper();
    private String BaseURI = "http://localhost:5000";
    private String RequestURI = "/WeatherForecast";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    @GetMapping("/getWeather")
    public String getWeather() throws Exception {
        final long start = System.currentTimeMillis();

        Future<List<WeatherForecast>> forecasts = getWeatherAsyncRestClient();

        while (!forecasts.isDone()) {
            System.out.println("Calculating...");
            Thread.sleep(300);
            if ((System.currentTimeMillis() - start) > 1500) {
                forecasts.cancel(true);
            }
        }

        log.info("Elapsed time: {}", (System.currentTimeMillis() - start));
        return String.format(forecasts.isCancelled() ? "Cancelled" : forecasts.get().toString());
    }

    public Future<List<WeatherForecast>> getWeatherSyncRestClient() { //not cancelling the request
        return executor.submit(() -> {
            ResponseEntity<Object[]> responseEntity =
                    restTemplate.getForEntity(BaseURI + RequestURI, Object[].class);

            Object[] objects = responseEntity.getBody();

            if (Thread.currentThread().isInterrupted()) {
                return new ArrayList<WeatherForecast>();
            }

            return objectsToWeatherForecasts(objects);
        });
    }
    public Future<List<WeatherForecast>> getWeatherAsyncRestClient() { //not cancelling the request
        return executor.submit(() -> {
            ListenableFuture<ResponseEntity<Object[]>> responseEntity =
                    asyncRestTemplate.getForEntity(BaseURI + RequestURI, Object[].class);

            Object[] objects = responseEntity.get().getBody();

            if (Thread.currentThread().isInterrupted()) {
                return new ArrayList<WeatherForecast>();
            }

            return objectsToWeatherForecasts(objects);
        });
    }

    public Future<List<WeatherForecast>> getWeatherAsyncWebClient() { //Works!
        return executor.submit(() -> {

            WebClient client = WebClient.create(BaseURI);
            Mono<Object[]> response = client.get().uri(RequestURI)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object[].class).log();
            Object[] monoObjects = response.block();

            return objectsToWeatherForecasts(monoObjects);
        });
    }

    private List<WeatherForecast> objectsToWeatherForecasts(Object[] objects) {
        var weatherForecast = Arrays.stream(objects)
                .map(object -> mapper.convertValue(object, WeatherForecast.class))
                .collect(Collectors.toList());

        for (int i = 0; i < weatherForecast.stream().count(); i++) {
            log.info(weatherForecast.get(i).toString());
        }

        return weatherForecast;
    }
}
