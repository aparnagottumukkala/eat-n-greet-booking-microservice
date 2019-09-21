package com.nus.iss.eatngreet.booking.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.nus.iss.eatngreet.booking.entity.ItemEntity;
import com.nus.iss.eatngreet.booking.repository.ItemRepository;
import com.nus.iss.eatngreet.booking.responsedto.DataResponseDTO;
import com.nus.iss.eatngreet.booking.service.AutoSuggestService;
import com.nus.iss.eatngreet.booking.util.ResponseUtil;
import com.nus.iss.eatngreet.booking.util.Util;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AutoSuggestServiceImpl implements AutoSuggestService {

	@Autowired
	private ItemRepository itemRepository;

	public DataResponseDTO getItems(String itemName) {
		log.info("getFromAreaPincodes() of AutoSuggestServiceImpl.");
		DataResponseDTO response = new DataResponseDTO();
		int count = 30;
		if (Util.isStringEmpty(itemName)) {
			response.setData(null);
			log.info("Pincode entered by user is either empty or null.");
			ResponseUtil.prepareResponse(response, "Pincode can't be empty or null.", "failure",
					"Pincode entered by user is either empty or null.", false);
		} else {
			try {
				List<ItemEntity> items = itemRepository.itemNameAutoSuggest(itemName, PageRequest.of(0, count));
				if (!Util.isListEmpty(items)) {
					HashMap<Object, Object> data = new HashMap<>();
					List<Object> itemsList = new ArrayList<>();
					for (ItemEntity item : items) {
						HashMap<String, Object> record = new HashMap<>();
						record.put("id", item.getItemId());
						record.put("name", item.getName());
						record.put("cuisine", item.getCuisine());
						record.put("description", item.getDescription());
						itemsList.add(record);
					}
					data.put("items", itemsList);
					response.setData(data);
					log.info("Successfully fetched data.");
					ResponseUtil.prepareResponse(response, "Successfully fetched data.", "success",
							"Successful execution.", true);
				} else {
					log.info("No matching items found.");
					ResponseUtil.prepareResponse(response, "No matching items found.", "success",
							"No matching items found.", true);
				}
			} catch (Exception e) {
				response.setData(null);
				log.error("Exception occurred: " + e.getMessage());
				ResponseUtil.prepareResponse(response, "Some error occurred, please try again later.", "failure",
						e.getMessage(), false);
			}
		}
		return response;
	}

	@Override
	public DataResponseDTO getAllItems() {
		log.info("getFromAreaPincodes() of AutoSuggestServiceImpl.");
		DataResponseDTO response = new DataResponseDTO();
		try {
			List<ItemEntity> items = itemRepository.findAll();
			if (!Util.isListEmpty(items)) {
				HashMap<Object, Object> data = new HashMap<>();
				List<Object> itemsList = new ArrayList<>();
				for (ItemEntity item : items) {
					HashMap<String, Object> record = new HashMap<>();
					record.put("id", item.getItemId());
					record.put("name", item.getName());
					record.put("cuisine", item.getCuisine());
					record.put("description", item.getDescription());
					itemsList.add(record);
				}
				data.put("items", itemsList);
				response.setData(data);
				log.info("Successfully fetched data.");
				ResponseUtil.prepareResponse(response, "Successfully fetched data.", "success", "Successful execution.",
						true);
			} else {
				log.info("No matching items found.");
				ResponseUtil.prepareResponse(response, "No matching items found.", "success",
						"No matching items found.", true);
			}
		} catch (Exception e) {
			response.setData(null);
			log.error("Exception occurred: " + e.getMessage());
			ResponseUtil.prepareResponse(response, "Some error occurred, please try again later.", "failure",
					e.getMessage(), false);
		}
		return response;
	}

}