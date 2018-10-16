package esa.s1pdgs.cpoc.mqi.server.distribution;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.ResumeDetails;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelProductDto;
import esa.s1pdgs.cpoc.mqi.model.rest.Ack;
import esa.s1pdgs.cpoc.mqi.model.rest.AckMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericPublicationMessageDto;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties;
import esa.s1pdgs.cpoc.mqi.server.GenericKafkaUtils;
import esa.s1pdgs.cpoc.mqi.server.consumption.MessageConsumptionController;
import esa.s1pdgs.cpoc.mqi.server.publication.MessagePublicationController;
import esa.s1pdgs.cpoc.mqi.server.test.RestControllerTest;

/**
 * Test the controller AuxiliaryFilesDistributionController
 * 
 * @author Viveris Technologies
 */
public class LevelProductDistributionControllerTest extends RestControllerTest {

    /**
     * Mock the controller of consumed messages
     */
    @Mock
    private MessageConsumptionController messages;

    /**
     * Mock the controller of published messages
     */
    @Mock
    private MessagePublicationController publication;

    /**
     * Mock the application properties
     */
    @Mock
    private ApplicationProperties properties;

    /**
     * The consumed messsage
     */
    private GenericMessageDto<LevelProductDto> consumedMessage;

    /**
     * The controller to test
     */
    private LevelProductDistributionController controller;

    /**
     * Initialization
     * @throws AbstractCodedException 
     */
    @Before
    public void init() throws AbstractCodedException {
        MockitoAnnotations.initMocks(this);

        LevelProductDto dto = new LevelProductDto("product-name", "key-obs",
                ProductFamily.L1_ACN, "NRT");
        consumedMessage =
                new GenericMessageDto<LevelProductDto>(123, "input-key", dto);

        doReturn(consumedMessage).when(messages).nextMessage(Mockito.any());

        doReturn(1000).when(properties).getWaitNextMs();

        controller = new LevelProductDistributionController(messages,
                publication, properties);

        this.initMockMvc(this.controller);
    }

    /**
     * Test the URI of the next message API
     * 
     * @throws Exception
     */
    @Test
    public void testNextMessageUri() throws Exception {
        request(get("/messages/level_products/next"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.identifier", is(123)))
                .andExpect(jsonPath("$.inputKey",
                        is(consumedMessage.getInputKey())))
                .andExpect(jsonPath("$.body.keyObjectStorage", is("key-obs")))
                .andExpect(jsonPath("$.body.productName", is("product-name")))
                .andExpect(jsonPath("$.body.family", is("L1_ACN")));
        verify(messages, times(1))
                .nextMessage(Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
    }

    /**
     * Test the URI of the ack message API
     * 
     * @throws Exception
     */
    @Test
    public void testAckMessageUri() throws Exception {
        doReturn(new ResumeDetails("topic", "dto-obj")).when(messages).ackMessage(Mockito.any(),
                Mockito.eq(123L), Mockito.any(), Mockito.anyBoolean());
        //TODO update
        doReturn(new ResumeDetails("topic", "dto-obj")).when(messages).ackMessage(Mockito.any(),
                Mockito.eq(312L), Mockito.any(), Mockito.anyBoolean());
        doReturn(true).when(publication).publishError(Mockito.any());

        String dto1 = GenericKafkaUtils.convertObjectToJsonString(
                new AckMessageDto(123, Ack.OK, null, false));
        String dto2 = GenericKafkaUtils.convertObjectToJsonString(
                new AckMessageDto(321, Ack.ERROR, "Error log", false));

        request(post("/messages/level_products/ack")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(dto1))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().string("true"));
        verify(messages, times(1)).ackMessage(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS), Mockito.eq(123L),
                Mockito.eq(Ack.OK), Mockito.eq(false));

        request(post("/messages/level_products/ack")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(dto2))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().string("true"));
        verify(messages, times(1)).ackMessage(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS), Mockito.eq(321L),
                Mockito.eq(Ack.ERROR), Mockito.eq(false));
        verify(publication, times(1)).publishError(Mockito.eq("Error log"));
        verifyNoMoreInteractions(messages);
    }

    /**
     * Test the URI of the publish message API
     * 
     * @throws Exception
     */
    @Test
    public void testPublishMessageUri() throws Exception {
        doNothing().when(publication).publish(Mockito.any(), Mockito.any());
        GenericPublicationMessageDto<LevelProductDto> dto =
                new GenericPublicationMessageDto<>(ProductFamily.L0_SLICE,
                        new LevelProductDto("product-name", "key-test",
                                ProductFamily.L0_SLICE, "NRT"));
        String convertedObj = GenericKafkaUtils.convertObjectToJsonString(dto);
        request(post("/messages/level_products/publish")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertedObj))
                        .andExpect(MockMvcResultMatchers.status().isOk());
        verify(publication, times(1)).publish(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS),
                Mockito.eq(dto.getMessageToPublish()));
    }
}
