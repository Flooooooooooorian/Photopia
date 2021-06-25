package de.neuefische.flooooooooooorian.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuefische.flooooooooooorian.backend.dto.LocationCreationDto;
import de.neuefische.flooooooooooorian.backend.model.Location;
import de.neuefische.flooooooooooorian.backend.model.Picture;
import de.neuefische.flooooooooooorian.backend.repository.LocationRepository;
import de.neuefische.flooooooooooorian.backend.repository.PictureRepository;
import de.neuefische.flooooooooooorian.backend.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PictureRepository pictureRepository;

    @MockBean
    private CloudinaryService cloudinaryService;

    @BeforeEach
    public void clearDb() {
        locationRepository.deleteAll();
    }

    @Test
    void getBasicLocationsControllerIntegrationTest() {
        Picture p1 = Picture.builder()
                .id("feraegdarg")
                .url("www.url1.com")
                .build();

        Picture p2 = Picture.builder()
                .id("sdofs")
                .url("www.url2.com")
                .build();


        Location l1 = Location.builder()
                .lat(50.0)
                .lng(15)
                .id("dsfdsfg4eyt")
                .description("description l1")
                .title("title")
                .thumbnail(p1)
                .build();
        Location l2 = Location.builder()
                .lat(10.46484)
                .lng(1.648)
                .id("fsdfnaldgadgd")
                .description("description l2")
                .title("title")
                .thumbnail(p2)
                .build();

        pictureRepository.save(p1);
        pictureRepository.save(p2);
        locationRepository.save(l1);
        locationRepository.save(l2);

        ResponseEntity<Location[]> response = testRestTemplate.getForEntity("http://localhost:" + port + "/api/location", Location[].class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody(), arrayContainingInAnyOrder(l1, l2));
    }

    @Test
    void getBasicLocationByIdControllerIntegrationTest() {
        Picture p1 = Picture.builder()
                .id("feraegdarg")
                .url("www.url1.com")
                .build();

        Picture p2 = Picture.builder()
                .id("sdofs")
                .url("www.url2.com")
                .build();


        Location l1 = Location.builder()
                .lat(50.0)
                .lng(15)
                .id("dsfdsfg4eyt")
                .description("description l1")
                .title("title")
                .thumbnail(p1)
                .build();
        Location l2 = Location.builder()
                .lat(10.46484)
                .lng(1.648)
                .id("fsdfnaldgadgd")
                .description("description l2")
                .title("title")
                .thumbnail(p2)
                .build();

        pictureRepository.save(p1);
        pictureRepository.save(p2);
        locationRepository.save(l1);
        locationRepository.save(l2);

        ResponseEntity<Location> response = testRestTemplate.getForEntity("http://localhost:" + port + "/api/location/" + l2.getId(), Location.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody(), is(l2));
    }

    @Test
    void getLocationsWithGeoLocation() throws IOException {
        Picture p1 = Picture.builder()
                .id("feraegdarg")
                .url("www.url1.com")
                .build();

        Picture p2 = Picture.builder()
                .id("sdofs")
                .url("www.url2.com")
                .build();


        Location l1 = Location.builder()
                .lat(50.0)
                .lng(48)
                .id("dsfdsfg4eyt")
                .description("description l1")
                .title("title")
                .thumbnail(p1)
                .build();
        Location l2 = Location.builder()
                .lat(10.46484)
                .lng(1.648)
                .id("fsdfnaldgadgd")
                .description("description l2")
                .title("title")
                .thumbnail(p2)
                .build();

        pictureRepository.save(p1);
        pictureRepository.save(p2);
        locationRepository.save(l1);
        locationRepository.save(l2);

        ResponseEntity<Location[]> response = testRestTemplate.getForEntity("http://localhost:" + port + "/api/location?lat=50&lng=50", Location[].class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), arrayContainingInAnyOrder(l1));
    }

    @Test
    void createBasicLocationControllerIntegrationTest() throws IOException {
        Picture picture = Picture.builder().url("testurl").id("fsfsdf").build();

        when(cloudinaryService.uploadImage(Mockito.any(File.class))).thenReturn(picture);

        LocationCreationDto dto = LocationCreationDto.builder()
                .lat(50.0)
                .lng(15)
                .description("description l1")
                .title("title")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_MIXED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("locationCreationDto", dto);
        body.add("file", new ClassPathResource("test_img.jpg"));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Location> response = testRestTemplate.exchange("http://localhost:" + port + "/api/location/", HttpMethod.POST, requestEntity, Location.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getId(), notNullValue());
        assertThat(response.getBody().getThumbnail().getId(), notNullValue());
        assertThat(response.getBody(), is(Location
                .builder()
                .id(response.getBody().getId())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .title(dto.getTitle())
                .thumbnail(Picture.builder().id(response.getBody().getThumbnail().getId()).url("testurl").build())
                .description(dto.getDescription())
                .build()));
    }

    @Test
    void createBasicLocationWithoutThumbnailControllerIntegrationTest() {
        LocationCreationDto dto = LocationCreationDto.builder()
                .lat(50.0)
                .lng(15)
                .description("description l1")
                .title("title")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_MIXED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("locationCreationDto", dto);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Location> response = testRestTemplate.exchange("http://localhost:" + port + "/api/location/", HttpMethod.POST, requestEntity, Location.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getId(), notNullValue());
        assertThat(response.getBody(), is(Location
                .builder()
                .id(response.getBody().getId())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .build()));
    }
}
