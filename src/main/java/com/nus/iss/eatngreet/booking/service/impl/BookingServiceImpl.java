package com.nus.iss.eatngreet.booking.service.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.nus.iss.eatngreet.booking.entity.ConsumerOrderEntity;
import com.nus.iss.eatngreet.booking.entity.ItemEntity;
import com.nus.iss.eatngreet.booking.entity.ProducerOrderEntity;
import com.nus.iss.eatngreet.booking.loggerservice.ApplicationLogger;
import com.nus.iss.eatngreet.booking.repository.ConsumerOrderRepository;
import com.nus.iss.eatngreet.booking.repository.ItemRepository;
import com.nus.iss.eatngreet.booking.repository.ProducerOrderRepository;
import com.nus.iss.eatngreet.booking.requestdto.ConsumerOrderRequestDto;
import com.nus.iss.eatngreet.booking.requestdto.ProducerOrderRequestDto;
import com.nus.iss.eatngreet.booking.responsedto.CommonResponseDTO;
import com.nus.iss.eatngreet.booking.responsedto.ConsumerOrderResponseDto;
import com.nus.iss.eatngreet.booking.responsedto.DataResponseDTO;
import com.nus.iss.eatngreet.booking.responsedto.ProducerOrderResponseDto;
import com.nus.iss.eatngreet.booking.restcontroller.BookingRestController;
import com.nus.iss.eatngreet.booking.service.BookingService;
import com.nus.iss.eatngreet.booking.util.Constants;
import com.nus.iss.eatngreet.booking.util.ResponseUtil;
import com.nus.iss.eatngreet.booking.util.Util;

@Service
public class BookingServiceImpl implements BookingService {

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	ProducerOrderRepository producerOrderRepository;

	@Autowired
	ConsumerOrderRepository consumerOrderRepository;

	@Value("${eatngreet.usermicroservice.url.domain}")
	private String userMicroserviceDomain;

	@Value("${eatngreet.usermicroservice.url.port}")
	private String userMicroservicePort;

	private CommonResponseDTO commonResponseDTO;

	@Override
	public CommonResponseDTO createProducerOrder(HttpServletRequest request, ProducerOrderRequestDto producerOrder) {
		commonResponseDTO = new CommonResponseDTO();
		try {
			ApplicationLogger.logMessage("Starting BookingServiceImpl for createOrder", BookingRestController.class);
			if (checkValidity(producerOrder, commonResponseDTO)) {
				ProducerOrderEntity producerOrderEntity = createProducerOrderEntity(request, producerOrder);
				producerOrderRepository.save(producerOrderEntity);
				ResponseUtil.prepareResponse(commonResponseDTO, "Successfully registered.", "SUCCESS",
						"Successfully registered.", true);
			}
		} catch (Exception ex) {
			ResponseUtil.prepareResponse(commonResponseDTO, "Not Registered Successfully", "FAILURE",
					"Following exception occurred " + ex.getMessage(), false);
		}
		return commonResponseDTO;
	}

	private ProducerOrderEntity createProducerOrderEntity(HttpServletRequest request,
			ProducerOrderRequestDto producerOrder) {
		ProducerOrderEntity producerOrderEntity = new ProducerOrderEntity();
		String email = getDecryptedEmail(request);
		if (Util.isValidEmail(email)) {
			producerOrderEntity.setEmail(email);
		}
		producerOrderEntity.setPaymentDeadline(calculatePaymentDeadline(producerOrder.getReservationDeadline()));
		producerOrderEntity.setMaxPeopleCount(producerOrder.getMaxPeopleCount());
		producerOrderEntity.setPreferenceType(producerOrder.getPreferenceType());
		producerOrderEntity.setPrice(producerOrder.getPrice());
		producerOrderEntity.setReservationDeadline(producerOrder.getReservationDeadline());
		producerOrderEntity.setServingDate(producerOrder.getServingDate());
		producerOrderEntity.setOtherItems(producerOrder.getOtherItems());
		producerOrderEntity.setNote(producerOrder.getNote());
		producerOrderEntity.setIsActive(true);
		producerOrderEntity.setIsDeleted(false);
		Set<ItemEntity> itemSet = getItems(producerOrder);
		if (!itemSet.isEmpty()) {
			producerOrderEntity.setItemList(itemSet);
		}
		return producerOrderEntity;
	}

	private String getDecryptedEmail(HttpServletRequest request) {
		String authToken = request.getHeader("Authorization").substring("Basic".length()).trim();
		String decryptedEmail = new String(Base64.getDecoder().decode(authToken)).split(":")[0];
		return decryptedEmail;
	}

	private Set<ItemEntity> getItems(ProducerOrderRequestDto producerOrder) {
		Set<ItemEntity> itemSet = new HashSet<ItemEntity>();
		for (Long itemId : producerOrder.getItemList()) {
			if (!Util.isLongValueEmpty(itemId)) {
				List<ItemEntity> itemList = itemRepository.findByItemId(itemId);
				if (!itemList.isEmpty()) {
					itemSet.add(itemList.get(0));
				}
			}
		}
		return itemSet;
	}

	private boolean checkValidity(ProducerOrderRequestDto producerOrder, CommonResponseDTO commonResponseDTO) {
		if (Util.isListEmpty(producerOrder.getItemList())) {
			ResponseUtil.prepareResponse(commonResponseDTO, "Not Registered Successfully : No Items Selected ",
					"FAILURE", "No Items Selected ", false);
			return false;
		} else if (producerOrder.getMaxPeopleCount() == null
				|| Util.isLongValueEmpty(producerOrder.getMaxPeopleCount())) {
			ResponseUtil.prepareResponse(commonResponseDTO, "Not Registered Successfully : Max People Count not set ",
					"FAILURE", "Max People Count not set ", false);
			return false;
		} else if (producerOrder.getPreferenceType() == null || Util.isLongValueEmpty(producerOrder.getPrice())) {
			ResponseUtil.prepareResponse(commonResponseDTO,
					"Not Registered Successfully : Preference type not selected", "FAILURE",
					"Preference type not selected", false);
			return false;
		} else if (producerOrder.getReservationDeadline() == null) {
			ResponseUtil.prepareResponse(commonResponseDTO,
					"Not Registered Successfully : Reservation deadline not set", "FAILURE",
					"Reservation deadline not set", false);
			return false;
		} else if (producerOrder.getServingDate() == null) {
			ResponseUtil.prepareResponse(commonResponseDTO, "Not Registered Successfully : Serving date not set",
					"FAILURE", "Serving date not set", false);
			return false;
		}
		return true;
	}

	private Date calculatePaymentDeadline(Date reservationDeadline) {
		return Util.getDateReducedByHours(reservationDeadline, Constants.six);
	}

	@Override
	public CommonResponseDTO createConsumerOrder(HttpServletRequest request, ConsumerOrderRequestDto consumerOrder) {
		commonResponseDTO = new CommonResponseDTO();
		try {
			ApplicationLogger.logMessage("Starting BookingServiceImpl for createConsumerOrder",
					BookingRestController.class);
			if (!Util.isLongValueEmpty(consumerOrder.getProducerOrderId())) {
				ConsumerOrderEntity consumerOrderEntity = createConsumerOrderEntity(request, consumerOrder,
						commonResponseDTO);
				consumerOrderRepository.save(consumerOrderEntity);
				ResponseUtil.prepareResponse(commonResponseDTO, "Successfully registered.", "SUCCESS",
						"Successfully registered.", true);
			} else {
				ResponseUtil.prepareResponse(commonResponseDTO, "Producer order Id missing", "FAILURE",
						"Producer order Id missing", false);
			}
		} catch (Exception ex) {
			ResponseUtil.prepareResponse(commonResponseDTO, "Not Registered Successfully", "FAILURE",
					"Following exception occurred " + ex.getMessage(), false);
		}
		return commonResponseDTO;
	}

	private ConsumerOrderEntity createConsumerOrderEntity(HttpServletRequest request,
			ConsumerOrderRequestDto consumerOrder, CommonResponseDTO commonResponseDTO) {
		ConsumerOrderEntity consumerOrderEntity = null;
		List<ProducerOrderEntity> producerOrderEntityList = producerOrderRepository
				.findByProducerOrderId(consumerOrder.getProducerOrderId());
		if (!producerOrderEntityList.isEmpty()) {
			ProducerOrderEntity producerOrderEntity = producerOrderEntityList.get(0);
			if (checkPeopleCount(producerOrderEntity)) {
				String email = getDecryptedEmail(request);
				if (Util.isValidEmail(email)) {
					if (!email.equals(producerOrderEntity.getEmail())) {
						producerOrderEntity
								.setActualPeopleCount(producerOrderEntity.getActualPeopleCount() + Constants.one);
						consumerOrderEntity = new ConsumerOrderEntity();
						consumerOrderEntity.setProducerOrderEntity(producerOrderEntity);
						consumerOrderEntity.setEmail(email);
					} else {
						ResponseUtil.prepareResponse(commonResponseDTO, "Producer cannot book his own meal ", "FAILURE",
								"Producer cannot book his own meal ", false);
					}
				} else {
					ResponseUtil.prepareResponse(commonResponseDTO, "Invalid email id ", "FAILURE", "Invalid email id ",
							false);
				}
			} else {
				ResponseUtil.prepareResponse(commonResponseDTO,
						"Not Registered Successfully : Maximum people count reached ", "FAILURE",
						"Maximum people count reached ", false);
			}
		}
		return consumerOrderEntity;
	}

	private boolean checkPeopleCount(ProducerOrderEntity producerOrderEntity) {
		boolean countAcceptable = false;
		if (producerOrderEntity.getActualPeopleCount() < producerOrderEntity.getMaxPeopleCount()) {
			countAcceptable = true;
		}
		return countAcceptable;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommonResponseDTO fetchAllProducerOrders() {
		ApplicationLogger.logMessage("Starting BookingServiceImpl for fetch all producer orders",
				BookingRestController.class);
		commonResponseDTO = new CommonResponseDTO();
		Integer count = 30;
		List<ProducerOrderEntity> prodEntities = producerOrderRepository
				.findAllProducerRecords(PageRequest.of(0, count));
		if (!prodEntities.isEmpty()) {
			DataResponseDTO dataResponseDTO = callUserMicroserviceForOrders(prodEntities);
			if (dataResponseDTO.getSuccess()) {
				Map<String, Object> userResponse = dataResponseDTO.getData();
				Map<String, List<ProducerOrderResponseDto>> responseMap = getResponseMap(
						(Map<String, Object>) userResponse.get("userInfo"), prodEntities);
				commonResponseDTO.setData(responseMap);
			}

		}
		return commonResponseDTO;
	}

	private DataResponseDTO callUserMicroserviceForOrders(List<ProducerOrderEntity> prodEntities) {
		Map<String, Set<String>> emailIdMap = new HashMap<String, Set<String>>();
		Set<String> emailIdSet = new HashSet<String>();
		for (ProducerOrderEntity producerOrderEntity : prodEntities) {
			if (producerOrderEntity.getEmail() != null) {
				emailIdSet.add(producerOrderEntity.getEmail());
			}
		}
		emailIdMap.put("emailIds", emailIdSet);
		final String uri = userMicroserviceDomain + ":" + userMicroservicePort
				+ "/eatngreet/userms/user/get-users-info";
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");
		HttpEntity<Map<String, Set<String>>> entity = new HttpEntity<Map<String, Set<String>>>(emailIdMap, headers);
		RestTemplate restTemplate = new RestTemplate();
		DataResponseDTO dataResponseDTO = (DataResponseDTO) restTemplate.postForObject(uri, entity,
				DataResponseDTO.class);
		return dataResponseDTO;
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<ProducerOrderResponseDto>> getResponseMap(Map<String, Object> userAddressMap,
			List<ProducerOrderEntity> prodEntities) {
		Map<String, List<ProducerOrderResponseDto>> producerOrderResponseMap = new HashMap<String, List<ProducerOrderResponseDto>>();
		List<ProducerOrderResponseDto> prodOrderResponseDtoList = new ArrayList<ProducerOrderResponseDto>();
		if (!userAddressMap.isEmpty() && !prodEntities.isEmpty()) {
			for (ProducerOrderEntity prodEntity : prodEntities) {
				if (prodEntity.getEmail() != null) {
					Map<String, Object> nameAddressMap = (Map<String, Object>) userAddressMap
							.get(prodEntity.getEmail());
					ProducerOrderResponseDto producerOrderResponseDto = new ProducerOrderResponseDto();
					producerOrderResponseDto.setProducerOrderId(prodEntity.getProducerOrderId());
					if (!nameAddressMap.isEmpty()) {
						producerOrderResponseDto.setFirstName((String) nameAddressMap.get("firstName"));
						producerOrderResponseDto.setLastName((String) nameAddressMap.get("lastName"));
						producerOrderResponseDto.setAddress(nameAddressMap.get("address"));
					}
					producerOrderResponseDto.setItemList(prodEntity.getItemList());
					producerOrderResponseDto.setEmail(prodEntity.getEmail());
					producerOrderResponseDto.setImageUrls(getImageUrls(prodEntity.getItemList()));
					producerOrderResponseDto.setImageThumbnailUrls(getThumbnailImageUrls(prodEntity.getItemList()));
					producerOrderResponseDto.setActualPeopleCount(prodEntity.getActualPeopleCount());
					producerOrderResponseDto.setMaxPeopleCount(prodEntity.getMaxPeopleCount());
					producerOrderResponseDto.setNote(prodEntity.getNote());
					producerOrderResponseDto.setOtherItems(prodEntity.getOtherItems());
					producerOrderResponseDto.setPaymentDeadline(prodEntity.getPaymentDeadline());
					producerOrderResponseDto.setPreferenceType(prodEntity.getPreferenceType());
					producerOrderResponseDto.setPrice(prodEntity.getPrice());
					producerOrderResponseDto.setReservationDeadline(prodEntity.getReservationDeadline());
					producerOrderResponseDto.setServingDate(prodEntity.getServingDate());
					prodOrderResponseDtoList.add(producerOrderResponseDto);
				}
			}
		}
		producerOrderResponseMap.put("ProducerOrder", prodOrderResponseDtoList);
		return producerOrderResponseMap;
	}

	private List<String> getImageUrls(Set<ItemEntity> itemList) {
		List<String> imageUrls = new ArrayList<String>();
		for (ItemEntity itemEntity : itemList) {
			if (itemEntity.getImageURL() != null) {
				imageUrls.add(itemEntity.getImageURL());
			}
		}
		return imageUrls;
	}

	private List<String> getThumbnailImageUrls(Set<ItemEntity> itemList) {
		List<String> imageUrls = new ArrayList<String>();
		for (ItemEntity itemEntity : itemList) {
			if (itemEntity.getImageThumbnailURL() != null) {
				imageUrls.add(itemEntity.getImageThumbnailURL());
			}
		}
		return imageUrls;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommonResponseDTO fetchSingleItem(Long producerOrderId) {
		ApplicationLogger.logMessage("Starting BookingServiceImpl for fetching single producer order",
				BookingRestController.class);
		commonResponseDTO = new CommonResponseDTO();
		if (!Util.isLongValueEmpty(producerOrderId)) {
			List<ProducerOrderEntity> prodEntityList = producerOrderRepository.findByProducerOrderId(producerOrderId);
			if (!prodEntityList.isEmpty()) {
				ProducerOrderEntity prodEntity = prodEntityList.get(0);
				if (prodEntity != null) {
					List<ProducerOrderEntity> prodEntities = new ArrayList<ProducerOrderEntity>();
					prodEntities.add(prodEntity);
					DataResponseDTO dataResponseDTO = callUserMicroserviceForOrders(prodEntities);
					if (dataResponseDTO.getSuccess()) {
						Map<String, Object> userResponse = dataResponseDTO.getData();
						Map<String, List<ProducerOrderResponseDto>> responseMap = getResponseMap(
								(Map<String, Object>) userResponse.get("userInfo"), prodEntities);
						commonResponseDTO.setData(responseMap);
					}
				}
			}
		}
		return commonResponseDTO;
	}

	@Override
	public CommonResponseDTO fetchSingleConsumerItem(HttpServletRequest request) {
		commonResponseDTO = new CommonResponseDTO();
		Map<String, ConsumerOrderResponseDto> consumerOrderResponseMap = new HashMap<String, ConsumerOrderResponseDto>();
		String email = getDecryptedEmail(request);
		if (email != null) {
			List<ConsumerOrderEntity> consumerOrderEntityList = consumerOrderRepository.findByEmail(email);
			if (!consumerOrderEntityList.isEmpty()) {
				for (ConsumerOrderEntity consumerOrderEntity : consumerOrderEntityList) {
					ConsumerOrderResponseDto consumerOrderResponseDto = new ConsumerOrderResponseDto();
					consumerOrderResponseDto.setConsumerId(consumerOrderEntity.getConsumerId());
					if (consumerOrderEntity.getProducerOrderEntity() != null) {
						consumerOrderResponseDto
								.setItemList(consumerOrderEntity.getProducerOrderEntity().getItemList());
						consumerOrderResponseDto
								.setPaymentDeadline(consumerOrderEntity.getProducerOrderEntity().getPaymentDeadline());
						consumerOrderResponseDto
								.setServingDate(consumerOrderEntity.getProducerOrderEntity().getServingDate());
						consumerOrderResponseDto.setPrice(consumerOrderEntity.getProducerOrderEntity().getPrice());
						consumerOrderResponseMap.put("consumerOrder", consumerOrderResponseDto);
					}
				}
			}
		}
		commonResponseDTO.setData(consumerOrderResponseMap);
		return commonResponseDTO;
	}

	@Override
	public CommonResponseDTO fetchSingleProducerItem(HttpServletRequest request) {
		commonResponseDTO = new CommonResponseDTO();
		String email = getDecryptedEmail(request);
		List<ProducerOrderEntity> producerOrderEntityList = producerOrderRepository.findByEmail(email);
		Map<String, List<ProducerOrderResponseDto>> prodMap = new HashMap<String, List<ProducerOrderResponseDto>>();
		List<ProducerOrderResponseDto> producerOrderResponseDtoList = new ArrayList<ProducerOrderResponseDto>();
		if (email != null) {
			if (!producerOrderEntityList.isEmpty()) {
				for (ProducerOrderEntity producerOrderEntity : producerOrderEntityList) {
					ProducerOrderResponseDto producerOrderResponseDto = new ProducerOrderResponseDto();
					producerOrderResponseDto.setActualPeopleCount(producerOrderEntity.getActualPeopleCount());
					producerOrderResponseDto.setMaxPeopleCount(producerOrderEntity.getMaxPeopleCount());
					producerOrderResponseDto.setItemList(producerOrderEntity.getItemList());
					producerOrderResponseDto.setOtherItems(producerOrderEntity.getOtherItems());
					producerOrderResponseDto.setImageUrls(getImageUrls(producerOrderEntity.getItemList()));
					producerOrderResponseDto
							.setImageThumbnailUrls(getThumbnailImageUrls(producerOrderEntity.getItemList()));
					producerOrderResponseDto.setNote(producerOrderEntity.getNote());
					producerOrderResponseDto.setServingDate(producerOrderEntity.getServingDate());
					producerOrderResponseDto.setPreferenceType(producerOrderEntity.getPreferenceType());
					producerOrderResponseDtoList.add(producerOrderResponseDto);
				}
			}
		}
		prodMap.put("producerOrders", producerOrderResponseDtoList);
		commonResponseDTO.setData(prodMap);
		return commonResponseDTO;
	}
}
