package esa.s1pdgs.cpoc.errorrepo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.result.DeleteResult;

import esa.s1pdgs.cpoc.appcatalog.common.MqiMessage;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.mqi.model.rest.LevelProductsMessageDto;

public class ErrorRepositoryTest {

	@Mock
	private MongoTemplate mongoTemplate;

	private ErrorRepository errorRepository;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		this.errorRepository = new ErrorRepositoryImpl(mongoTemplate);
	}

	@Test
	public void getFailedProcessings() {

		List<FailedProcessingDto<LevelProductsMessageDto>> failedProcessingsToReturn = new ArrayList<>();

		LevelProductsMessageDto levelProductsMsgDto = new LevelProductsMessageDto();
		FailedProcessingDto<LevelProductsMessageDto> fpDto = new FailedProcessingDto<>();
		fpDto.setIdentifier(123);
		fpDto.setDto(levelProductsMsgDto);
		failedProcessingsToReturn.add(fpDto);

		doReturn(failedProcessingsToReturn).when(mongoTemplate).findAll(FailedProcessingDto.class);

		List<FailedProcessingDto> failedProcessings = errorRepository.getFailedProcessings();

		assertEquals(123, failedProcessings.get(0).getIdentifier());
		assertTrue(failedProcessings.get(0).getDto() instanceof LevelProductsMessageDto);
	}

	@Test
	public void getFailedProcessingById() {

		LevelProductsMessageDto levelProductsMsgDto = new LevelProductsMessageDto();
		FailedProcessingDto<LevelProductsMessageDto> fpDtoToReturn = new FailedProcessingDto<>();
		fpDtoToReturn.setIdentifier(123);
		fpDtoToReturn.setDto(levelProductsMsgDto);

		doReturn(fpDtoToReturn).when(mongoTemplate).findById("123", FailedProcessingDto.class);

		FailedProcessingDto<LevelProductsMessageDto> failedProcessing = errorRepository.getFailedProcessingsById("123");

		assertEquals(123, failedProcessing.getIdentifier());
		assertTrue(failedProcessing.getDto() instanceof LevelProductsMessageDto);
	}
	
	@Test
	public void deleteFailedProcessing( ) {
		
		LevelProductsMessageDto levelProductsMsgDto = new LevelProductsMessageDto();
		FailedProcessingDto<LevelProductsMessageDto> fpDto = new FailedProcessingDto<>();
		fpDto.setIdentifier(123);
		fpDto.setDto(levelProductsMsgDto);
		
		doReturn(fpDto).when(mongoTemplate).findById("123", FailedProcessingDto.class);
		DeleteResult deleteResult = new DeleteResult() {
			
			@Override
			public boolean wasAcknowledged() {
				return true;
			}
			
			@Override
			public long getDeletedCount() {
				
				return 1;
			}
		};
		
		doReturn(deleteResult).when(mongoTemplate).remove(fpDto);
		
		assertTrue(errorRepository.deleteFailedProcessing("123"));
		assertFalse(errorRepository.deleteFailedProcessing("4"));
	}
	
	@Test
	public void saveFailedProcessing() {
		
		LevelProductsMessageDto levelProductsMsgDto = new LevelProductsMessageDto();
		levelProductsMsgDto.setIdentifier(1);
		FailedProcessingDto<LevelProductsMessageDto> fpDto = new FailedProcessingDto<>();
		fpDto.setIdentifier(123);
		fpDto.setDto(levelProductsMsgDto);
		
		MqiMessage mqiMsg = new MqiMessage();		
				doReturn(mqiMsg).when(mongoTemplate).findOne(any(), eq(MqiMessage.class));

		errorRepository.saveFailedProcessing(fpDto);
		verify(mongoTemplate, times(1)).insert(fpDto);
		
	}
	
	@Test
	public void restartAndDeleteFailedProcessing() {
		//TODO
	}

}
