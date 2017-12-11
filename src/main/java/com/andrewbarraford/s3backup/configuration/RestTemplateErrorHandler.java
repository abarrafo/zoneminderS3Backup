package com.andrewbarraford.s3backup.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@Slf4j
class RestTemplateErrorHandler extends DefaultResponseErrorHandler {

    /**
     *  {@inheritDoc}
     */
    @Override
    public void handleError(final ClientHttpResponse response) throws IOException {

        final HttpStatus status = response.getStatusCode();
        if(status == HttpStatus.NOT_FOUND){
            log.error("Error occurred working with remote service. 404");
        }else{
            super.handleError(response);
        }

    }


}
