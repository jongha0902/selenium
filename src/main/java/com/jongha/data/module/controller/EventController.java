package com.jongha.data.module.controller;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.jongha.data.module.service.EventService;

@Controller("EventController")
public class EventController {

	
	@Resource(name="eventService")
	private EventService eventService;
		
	Logger log = LoggerFactory.getLogger(" 크롤링 ");
	
	
	/**
	 * 크롤링
	 * @throws Exception 
	 */
	public void selenium() throws Exception{
		try{
//			서비스단 로직 실행
		}catch(Exception e){
			
		}
	}
	
	
}
