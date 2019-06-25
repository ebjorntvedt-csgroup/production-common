package esa.s1pdgs.cpoc.appcatalog.client.job;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobDto;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobGenerationDto;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;

/**
 * Implementation of job applicative data client for LEVEL_PRODUCTS product
 * category
 * 
 * @author Viveris Technologies
 */
public class LevelSegmentsAppCatalogJobService
        extends AbstractAppCatalogJobService<ProductDto> {

    /**
     * @param restTemplate
     * @param hostUri
     * @param maxRetries
     * @param tempoRetryMs
     */
    public LevelSegmentsAppCatalogJobService(final RestTemplate restTemplate,
            final String hostUri, final int maxRetries,
            final int tempoRetryMs) {
        super(restTemplate, hostUri, maxRetries, tempoRetryMs,
                ProductCategory.LEVEL_SEGMENTS);
    }

    /**
     * @see super{@link #internalExchangeSearch(URI)}
     */
    @Override
    protected ResponseEntity<List<AppDataJobDto<ProductDto>>> internalExchangeSearch(
            final URI uri) {
        ResponseEntity<List<LevelSegmentAppDataJobDto>> response =
                restTemplate.exchange(uri, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<LevelSegmentAppDataJobDto>>() {
                        });
        List<AppDataJobDto<ProductDto>> body = new ArrayList<>();
        List<LevelSegmentAppDataJobDto> responseBody = response.getBody();
        if (!CollectionUtils.isEmpty(responseBody)) {
            for (LevelSegmentAppDataJobDto elt : responseBody) {
                body.add(elt);
            }
        }
        return new ResponseEntity<List<AppDataJobDto<ProductDto>>>(body,
                response.getStatusCode());
    }

    /**
     * @see super{@link #internalExchangeNewJob(String, AppDataJobDto)}
     */
    @Override
    protected ResponseEntity<AppDataJobDto<ProductDto>> internalExchangeNewJob(
            final String uri, final AppDataJobDto<ProductDto> body) {
        ResponseEntity<LevelSegmentAppDataJobDto> response =
                restTemplate.exchange(uri, HttpMethod.POST,
                        new HttpEntity<AppDataJobDto<ProductDto>>(body),
                        LevelSegmentAppDataJobDto.class);

        return new ResponseEntity<AppDataJobDto<ProductDto>>(
                response.getBody(), response.getStatusCode());
    }

    /**
     * @see super{@link #internalExchangePatchJob(String, AppDataJobDto)}
     */
    @Override
    protected ResponseEntity<AppDataJobDto<ProductDto>> internalExchangePatchJob(
            final String uri, final AppDataJobDto<ProductDto> body) {
        ResponseEntity<LevelSegmentAppDataJobDto> response =
                restTemplate.exchange(uri, HttpMethod.PATCH,
                        new HttpEntity<AppDataJobDto<ProductDto>>(body),
                        LevelSegmentAppDataJobDto.class);

        return new ResponseEntity<AppDataJobDto<ProductDto>>(
                response.getBody(), response.getStatusCode());
    }

    /**
     * @see super{@link #internalExchangePatchTaskTableOfJob(String, AppDataJobGenerationDto)}
     */
    @Override
    protected ResponseEntity<AppDataJobDto<ProductDto>> internalExchangePatchTaskTableOfJob(
            final String uri, final AppDataJobGenerationDto body) {
        ResponseEntity<LevelSegmentAppDataJobDto> response =
                restTemplate.exchange(uri, HttpMethod.PATCH,
                        new HttpEntity<AppDataJobGenerationDto>(body),
                        LevelSegmentAppDataJobDto.class);

        return new ResponseEntity<AppDataJobDto<ProductDto>>(
                response.getBody(), response.getStatusCode());
    }

}

/**
 * @author Viveris Technologies
 */
class LevelSegmentAppDataJobDto extends AppDataJobDto<ProductDto> {

}
