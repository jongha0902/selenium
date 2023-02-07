package com.selenium.data.module.service.impl;

import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.ibatis.io.Resources;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service("eventService")
public class EventServiceImpl implements com.selenium.data.module.service.EventService{

	
		private WebDriver driver;
		private WebElement element;
		
		public final String WEB_DRIVER_ID = "";		// 클라이언트 PC에서 사용할 WEB 드라이버 종류
		public final String WEB_DRIVER_PATH ="";	// 클라이언트 PC에 깔려있는 WEB 드라이버 파일 위치
		
		@Autowired
		private Environment env;
		
		private Properties prop = new Properties();

		@Resource(name = "eventMapper")
	    private EventMapper eventDAO;

		// 한전 파워플래너 사용량 크롤링 등록
		public void powerPlanerUsageCrawling() throws Exception{
			crawl(); // 크롤링 및 데이터 등록
		}

		public void crawl() throws Exception{
			try{
				// application 프로퍼티에 정보들을 담아놓고 호출하기 위함 S
				prop = new Properties();
				String resource = "application.properties";
				Reader reader = Resources.getResourceAsReader(resource);
				prop.load(reader);
				
				// application 프로퍼티에 정보들을 담아놓고 호출하기 위함 S
				System.setProperty(prop.getProperty("WEB_DRIVER_ID"),prop.getProperty("WEB_DRIVER_PATH"));

				
				ChromeOptions chromeOptions = new ChromeOptions();
				
				
				chromeOptions.addArguments("--headless");
				
				chromeOptions.addArguments("--no-sandbox");			
				chromeOptions.addArguments("--disable-gpu");
				chromeOptions.addArguments("disable-infobars");
				chromeOptions.addArguments("--disable-extensions");
				chromeOptions.addArguments("start-maximized");
				chromeOptions.addArguments("disable-notifications");
				chromeOptions.addArguments("allow-running-insecure-content");
				chromeOptions.addArguments("--disable-extensions");
				chromeOptions.addArguments("--test-type");
				chromeOptions.addArguments("--ignore-certificate-errors");
				chromeOptions.addArguments("--disable-dev-shm-usage");				
								
				driver = new ChromeDriver(chromeOptions);
				
				JavascriptExecutor js = (JavascriptExecutor) driver;				
				
				driver.get("주소");
				
				// ID 엘리먼트 서칭 및 값대입
				element = driver.findElement(By.id("RSA_USER_ID"));
				Thread.sleep(500); // ID를 쓰고 PASS를 찾기전 텀을줌
				element.sendKeys(prop.getProperty("id"));
				
				// PASS 엘리먼트 서칭 및 값대입
				element = driver.findElement(By.id("RSA_USER_PWD"));
				element.sendKeys(prop.getProperty("pw"));
				
				// 로그인 버튼 클릭
				element = driver.findElement(By.className("intro_btn"));
				element.click();
				Thread.sleep(1000);
				
				// 파싱할 페이지로 이동
				driver.get("url");
				Thread.sleep(1000);
				
				// 파싱 대상 테이블 데이터 세팅 script 실행
				js.executeScript("getSerarchChart()");
				Thread.sleep(5000);
				
				element = driver.findElement(By.className("time_bar"));
				String tempWebLastCheckTime = element.getText().substring(6);
				String[] lastCheckTime = tempWebLastCheckTime.split(" ");
				
				
				// 테이블 엘리먼트 대입
				element = driver.findElement(By.id("tableListChart"));
				
									
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				driver.close();
				
				//작업관리자 프로세스 제거
				Runtime.getRuntime().exec("taskkill /F /IM chrome.exe /T");
				Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
			}
		}
		
		public String removeComma(String data){
			return data.replaceAll("\\,", "");
		}

}
