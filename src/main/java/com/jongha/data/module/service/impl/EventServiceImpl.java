package com.jongha.data.module.service.impl;

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
public class EventServiceImpl implements com.jongha.data.module.service.EventService{

	
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
				
				//System.setProperty(WEB_DRIVER_ID,WEB_DRIVER_PATH);				
				//System.setProperty("webdriver.chrome.driver","C:\\workspace\\workspace_2020\\selenium\\chromedriver_win32\\chromedriver.exe");
				//System.setProperty("webdriver.chrome.driver","C:/workspace/workspace_2020/selenium/chromedriver_linux/chromedriver");
				
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
				
				driver.get("https://pp.kepco.co.kr/");
				
				// ID 엘리먼트 서칭 및 값대입
				element = driver.findElement(By.id("RSA_USER_ID"));
				Thread.sleep(500); // ID를 쓰고 PASS를 찾기전 텀을줌
				element.sendKeys(prop.getProperty("powerPlannerId"));
				
				// PASS 엘리먼트 서칭 및 값대입
				element = driver.findElement(By.id("RSA_USER_PWD"));
				element.sendKeys(prop.getProperty("powerPlannerPass"));
				
				// 로그인 버튼 클릭
				element = driver.findElement(By.className("intro_btn"));
				element.click();
				Thread.sleep(1000);
				
				// 파싱할 페이지로 이동
				driver.get("https://pp.kepco.co.kr/rs/rs0101N.do?menu_id=O010201");
				Thread.sleep(1000);
				
				// 파싱 대상 테이블 데이터 세팅 script 실행
				js.executeScript("getSerarchChart()");
				Thread.sleep(5000);
				
				element = driver.findElement(By.className("time_bar"));
				String tempWebLastCheckTime = element.getText().substring(6);
				String[] lastCheckTime = tempWebLastCheckTime.split(" ");
				
				//String[] lastCheckTime = {"2021년","03월","22일","(수)","00:30"};
				
				// 테이블 엘리먼트 대입
				element = driver.findElement(By.id("tableListChart"));
				
				String tableTxt = element.getText();
				
				Date today = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat df = new SimpleDateFormat("dd");
				
				String todayFormat = sdf.format(today);
				
				HashMap<String,Object> paramMap = new HashMap<String,Object>();
				paramMap.put("today",todayFormat);
							
				HashMap<String,Object> crawlingDataMap = new HashMap<String,Object>();
				HashMap<String,Object> getLastCheckTimeMap = eventDAO.getLastCheckTime(paramMap); // DB의 마지막 검침시간 가져오기
							
				
				String[] todaySplit = todayFormat.split("-");

				String[] newLineArr = tableTxt.split("\n");	// 개행처리 데이터 나눔 

				ArrayList<String> targetTimeArr = new ArrayList<String>();	
				
				
				String[] webLastCheckHourMinArr = (lastCheckTime[4]).split(":");  // web 최종 검침시간 [0]:시  [1]:분
				
				
				int tick = 0; // 몇번 insert를 시행해야 할지 계산
				
				if(getLastCheckTimeMap!=null){ // 금일 마지막 검침 시간이 있는지 확인
					System.out.println("================================1");
					if(!getLastCheckTimeMap.get("LAST_CHECK_TIME").equals(""+lastCheckTime[0]+" "+lastCheckTime[1]+" "+lastCheckTime[2]+" "+lastCheckTime[4])){ // 현재 웹페이지 최종검침시간과 DB의 마지막 검침시간이 같은지 확인 / 같지 않은 경우만 Insert
						if(getLastCheckTimeMap.get("LAST_CHECK_DAY").equals((lastCheckTime[2]).substring(0,2))){ // 최종 검침 일자와 DB의 마지막 검침 일자가 같은지 확인
							System.out.println("================================2");
							String[] dbLastCheckTime = getLastCheckTimeMap.get("LAST_CHECK_TIME").toString().split(" "); // 년월일을 떼어냄
							String[] dbLastCheckHourMinArr = (dbLastCheckTime[3]).split(":"); // db 최종 검침시간 [0]:시  [1]:분
	
							int hourDifferVal = Integer.parseInt(webLastCheckHourMinArr[0]) - Integer.parseInt(dbLastCheckHourMinArr[0]);
							int minDifferVal = Integer.parseInt(webLastCheckHourMinArr[1]) - Integer.parseInt(dbLastCheckHourMinArr[1]);
							
							if( hourDifferVal==0 ){ // 웹 최종 검침시간과 db 최종검침시간이 같은지
								targetTimeArr.clear();
								tick = minDifferVal/15;
								for(int i=0;i<tick;i++){								
									targetTimeArr.add( (Integer.parseInt(dbLastCheckHourMinArr[0]) > 9? dbLastCheckHourMinArr[0] : "0"+Integer.parseInt(dbLastCheckHourMinArr[0]))
											+":"+((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )) > 9? (Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )) : "0"+(Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))) );
								}							
								
							}else{ // 마지막 검침 시간의 시간기록과 DB 마지막 검침시간 시간기록이 달라졌을때
								System.out.println("================================3");
								targetTimeArr.clear();
								tick = ( (hourDifferVal*60)+minDifferVal )/15; // 시간차이를 분으로 환산하여 분차이와 합산 후 15분 주기로 나누어 횟수를 구함
								for(int i=0;i<tick;i++){				
									if( !((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )) >= 60) ){ // DB 마지막 검침시간의 분이 터해서 60분 이상이 아닐경우 그대로 사용
										targetTimeArr.add( ( Integer.parseInt(dbLastCheckHourMinArr[0])>9?dbLastCheckHourMinArr[0] : "0"+Integer.parseInt(dbLastCheckHourMinArr[0]))
															+":"+
															((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))>9?(Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )):"0"+(Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))));
									}else{									
										targetTimeArr.add( ((Integer.parseInt(dbLastCheckHourMinArr[0])+((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))/60) ) > 9 ? (Integer.parseInt(dbLastCheckHourMinArr[0])+((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))/60) ) : "0"+(Integer.parseInt(dbLastCheckHourMinArr[0])+((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))/60) ))
															+ ":" + 
															(((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )) - (((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))/60)*60) ) > 9 ? ((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )) - (((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))/60)*60) ) : "0"+((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) )) - (((Integer.parseInt(dbLastCheckHourMinArr[1])+( 15 * (i+1) ))/60)*60)))  
														); // 분계산시
									}
								}
							}					
								
							for(int j=0;j<newLineArr.length;j++){
								String[] tempArr = newLineArr[j].split(" "); // 데이터 각각으로 나눔
								for(int k=0;k<tempArr.length;k++){
									for(int z=0;z<targetTimeArr.size();z++){
										if(tempArr[k].equals(targetTimeArr.get(z))){
											
											String[] targetTimeSplitArr = targetTimeArr.get(z).split(":");
											
											//crawlingDataMap.put("lastCheckTime", tempWebLastCheckTime);
											crawlingDataMap.put("lastCheckTime", ""+lastCheckTime[0]+" "+lastCheckTime[1]+" "+lastCheckTime[2]+" "+lastCheckTime[4]);
											crawlingDataMap.put("targetDate", sdf.format(today));		// 금일 날짜
											crawlingDataMap.put("targetTime", targetTimeArr.get(z));	// 데이터 시간
											crawlingDataMap.put("targetHour", targetTimeSplitArr[0]);
											crawlingDataMap.put("targetMin", targetTimeSplitArr[1]);											
											crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
											crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
											crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
											crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
											crawlingDataMap.put("co2", tempArr[k+5]);
											crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
											crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);				
											
											eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
										}
									}
								}
								
							}
						}else{ // 웹 최종검침 일자와 DB의 검침 일자가 달라진 경우
							System.out.println("================================4");
							
							// ====================================전날 데이터 세팅 S==========================================
							
							targetTimeArr.clear();
							
							// 어제날짜 만들기
							Calendar cal = new GregorianCalendar(Locale.KOREAN);
							cal.setTime(today);
							cal.add(Calendar.DATE, -1);
							String yesterday = sdf.format(cal.getTime());
							
							paramMap.put("yesterday", yesterday);
							
							// 이전 날짜 마지막 데이터 확인
							HashMap<String,Object> getYesterDayDBLastHourMinMap = eventDAO.getYesterDayDBLastHourMin(paramMap);
						
							
							// 데이터피커 날짜를 어제로 세팅
							((JavascriptExecutor)driver).executeScript("document.getElementById('SELECT_DT').setAttribute('value','"+yesterday+"')");
																					
							Thread.sleep(1000);
							
							js.executeScript("getTotalData()"); // 조회 버튼 클릭 함수 실행
							
							Thread.sleep(5000);
							
							element = driver.findElement(By.id("tableListChart"));
							
							String yesterDayTableTxt = element.getText();
							
							String[] yesterDayNewLineArr = yesterDayTableTxt.split("\n");	// 어제 테이블 데이터 개행처리 데이터 나눔
							
							String[] yesterDaySplit = yesterday.split("-"); // 어제날짜를 년/월/일로 나눔
							
							if(getYesterDayDBLastHourMinMap!=null){	// 전날 마지막 데이터 시간 데이터가 있는지 확인						
								if((!getYesterDayDBLastHourMinMap.get("LOG_TIME").equals("24:00"))&&(lastCheckTime[2]).substring(0,2).equals(todaySplit[2])){ // 전날 마지막 데이터 시간이 맞는지 확인

									String[] yesterDaydbLastCheckTimeSplit = (getYesterDayDBLastHourMinMap.get("LOG_TIME")).toString().split(":"); // 어제날짜 DB 마지막 체크시간을 시/분 으로 나눔
																
									// 24시를 분으로 환산하여 DB마지막 체크시간을 분으로 환산한 값을 빼어 차이값을 만든후 빈 횟수를 만듬
									int yesterDayTick = ( (24*60) - ((Integer.parseInt(yesterDaydbLastCheckTimeSplit[0]) * 60) + Integer.parseInt(yesterDaydbLastCheckTimeSplit[1])) ) /15;
									
									int tempHourSum = Integer.parseInt(yesterDaydbLastCheckTimeSplit[0]); // DB 마지막 시간
									int tempMinSum = Integer.parseInt(yesterDaydbLastCheckTimeSplit[1]);  // DB 마지막 분
									
									for(int i=0;i<yesterDayTick;i++){
										if( (tempMinSum+15) >= 60 ){ // 분을 기준으로 이번 틱에 60분이 넘는지 확인
											tempMinSum = 0;
											tempHourSum++;							
										}else{
											tempMinSum+=15;
										}
										targetTimeArr.add( (tempHourSum > 9 ? tempHourSum : "0"+tempHourSum ) +":"+ (tempMinSum>9 ? tempMinSum : "0"+tempMinSum) ); 
									}
									
									
									for(int i=0;i<yesterDayNewLineArr.length;i++){
										String[] tempArr = yesterDayNewLineArr[i].split(" "); // 데이터 각각으로 나눔
										for(int j=0; j<targetTimeArr.size();j++){
											for(int k=0;k<tempArr.length;k++){
											
												if(targetTimeArr.get(j).equals(tempArr[k])){
													String[] dataTimeHourSplit = tempArr[k].split(":");
													
													crawlingDataMap.put("lastCheckTime", yesterDaySplit[0]+"년 "+yesterDaySplit[1]+"월 "+yesterDaySplit[2]+"일 "+targetTimeArr.get(j) );
													crawlingDataMap.put("targetDate", yesterday);								// 어제 날짜
													crawlingDataMap.put("targetTime", dataTimeHourSplit[0]+":"+dataTimeHourSplit[1]);	// 데이터 시간
													crawlingDataMap.put("targetHour", dataTimeHourSplit[0]);
													crawlingDataMap.put("targetMin", dataTimeHourSplit[1]);											
													crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
													crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
													crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
													crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
													crawlingDataMap.put("co2", tempArr[k+5]);
													crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
													crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);	
													
													eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
												}
											}
											
										}
									}									
								}
								
							}else{ // 전날 데이터가 아예 없을경우

								// 24시를 분으로 환산하여 DB마지막 체크시간을 분으로 환산한 값을 빼어 차이값을 만든후 빈 횟수를 만듬
								int yesterDayTick = (24*60) /15;
								
								int tempHourSum = 0; // 24시까지 채울 임시 시간 변수
								int tempMinSum = 0;  // 24시까지 채울 임시 분 변수
								
								for(int i=0;i<yesterDayTick;i++){
									if( (tempMinSum+15) >= 60 ){ // 분을 기준으로 이번 틱에 60분이 넘는지 확인
										tempMinSum = 0;
										tempHourSum++;							
									}else{
										tempMinSum+=15;
									}
									targetTimeArr.add( (tempHourSum > 9 ? tempHourSum : "0"+tempHourSum ) +":"+ (tempMinSum>9 ? tempMinSum : "0"+tempMinSum) ); 
								}
								
								
								for(int i=0;i<yesterDayNewLineArr.length;i++){
									String[] tempArr = yesterDayNewLineArr[i].split(" "); // 데이터 각각으로 나눔
									for(int j=0; j<targetTimeArr.size();j++){
										for(int k=0;k<tempArr.length;k++){
										
											if(targetTimeArr.get(j).equals(tempArr[k])){
												String[] dataTimeHourSplit = tempArr[k].split(":");
																						
												crawlingDataMap.put("lastCheckTime", yesterDaySplit[0]+"년 "+yesterDaySplit[1]+"월 "+yesterDaySplit[2]+"일 "+targetTimeArr.get(j) ); // 임의의 최종 검침 날짜를 만들어줌
												crawlingDataMap.put("targetDate", yesterday);										// 어제 날짜
												crawlingDataMap.put("targetTime", dataTimeHourSplit[0]+":"+dataTimeHourSplit[1]);	// 데이터 시간
												crawlingDataMap.put("targetHour", dataTimeHourSplit[0]);
												crawlingDataMap.put("targetMin", dataTimeHourSplit[1]);											
												crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
												crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
												crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
												crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
												crawlingDataMap.put("co2", tempArr[k+5]);
												crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
												crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);	
												
												eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
											}
										}
										
									}
								}
							}
							// ==================================== 전날 데이터 세팅 E =========================================						
							
							targetTimeArr.clear();
							if((df.format(today).toString()+"일").equals(lastCheckTime[2])){
								int tempHourSum = 0;
								int tempMinSum = 0;
																
								tick = ((Integer.parseInt(webLastCheckHourMinArr[0])*60) + Integer.parseInt(webLastCheckHourMinArr[1])) / 15;
								
								for(int i=0;i<tick;i++){
									if( (tempMinSum+15) >= 60 ){ // 분을 기준으로 이번 틱에 60분이 넘는지 확인
										tempMinSum = 0;
										tempHourSum++;							
									}else{
										tempMinSum+=15;
									}
									targetTimeArr.add( (tempHourSum > 9 ? tempHourSum : "0"+tempHourSum ) +":"+ (tempMinSum>9 ? tempMinSum : "0"+tempMinSum) ); 
								}
													
								for(int j=0;j<newLineArr.length;j++){
									String[] tempArr = newLineArr[j].split(" "); // 데이터 각각으로 나눔
									for(int z=0; z<targetTimeArr.size();z++){
										for(int k=0;k<tempArr.length;k++){
										
											if(targetTimeArr.get(z).equals(tempArr[k])){
												String[] dataTimeHourSplit = tempArr[k].split(":");
												
												crawlingDataMap.put("lastCheckTime", ""+lastCheckTime[0]+" "+lastCheckTime[1]+" "+lastCheckTime[2]+" "+lastCheckTime[4]);
												crawlingDataMap.put("targetDate", sdf.format(today));		// 금일 날짜
												crawlingDataMap.put("targetTime", dataTimeHourSplit[0]+":"+dataTimeHourSplit[1]);				// 데이터 시간
												crawlingDataMap.put("targetHour", dataTimeHourSplit[0]);
												crawlingDataMap.put("targetMin", dataTimeHourSplit[1]);											
												crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
												crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
												crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
												crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
												crawlingDataMap.put("co2", tempArr[k+5]);
												crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
												crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);	
												
												eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
											}
										}
										
									}
								}
							}
						}
					}
				}else{ // 금일 데이터 넣은게 없으면 모든 시간데이터를 insert
					
					// ====================================전날 데이터 세팅 S==========================================
					
					targetTimeArr.clear();
					

					// 어제날짜 만들기
					Calendar cal = new GregorianCalendar(Locale.KOREAN);
					cal.setTime(today);
					cal.add(Calendar.DATE, -1);
					String yesterday = sdf.format(cal.getTime());
					
					paramMap.put("yesterday",yesterday);
					
					// 이전 날짜 마지막 데이터 확인
					HashMap<String,Object> getYesterDayDBLastHourMinMap = eventDAO.getYesterDayDBLastHourMin(paramMap);
					
					
					// 데이터피커 날짜를 어제로 세팅
					((JavascriptExecutor)driver).executeScript("document.getElementById('SELECT_DT').setAttribute('value','"+yesterday+"')");
																			
					Thread.sleep(1000);
					
					js.executeScript("getTotalData()"); // 조회 버튼 클릭 함수 실행
					
					Thread.sleep(5000);
					
					element = driver.findElement(By.id("tableListChart"));
					
					String yesterDayTableTxt = element.getText();
					
					String[] yesterDayNewLineArr = yesterDayTableTxt.split("\n");	// 어제 테이블 데이터 개행처리 데이터 나눔
					
					String[] yesterDaySplit = yesterday.split("-"); // 어제날짜를 년/월/일로 나눔
					
					if(getYesterDayDBLastHourMinMap!=null){	// 전날 마지막 데이터 시간 데이터가 있는지 확인						
						if((!getYesterDayDBLastHourMinMap.get("LOG_TIME").equals("24:00"))&&(lastCheckTime[2]).substring(0,2).equals(todaySplit[2])){ // 전날 마지막 데이터 시간이 맞는지 확인
							
							String[] yesterDaydbLastCheckTimeSplit = (getYesterDayDBLastHourMinMap.get("LOG_TIME")).toString().split(":"); // 어제날짜 DB 마지막 체크시간을 시/분 으로 나눔
														
							// 24시를 분으로 환산하여 DB마지막 체크시간을 분으로 환산한 값을 빼어 차이값을 만든후 빈 횟수를 만듬
							int yesterDayTick = ( (24*60) - ((Integer.parseInt(yesterDaydbLastCheckTimeSplit[0]) * 60) + Integer.parseInt(yesterDaydbLastCheckTimeSplit[1])) ) /15;
							
							int tempHourSum = Integer.parseInt(yesterDaydbLastCheckTimeSplit[0]); // DB 마지막 시간
							int tempMinSum = Integer.parseInt(yesterDaydbLastCheckTimeSplit[1]);  // DB 마지막 분
							
							for(int i=0;i<yesterDayTick;i++){
								if( (tempMinSum+15) >= 60 ){ // 분을 기준으로 이번 틱에 60분이 넘는지 확인
									tempMinSum = 0;
									tempHourSum++;							
								}else{
									tempMinSum+=15;
								}
								targetTimeArr.add( (tempHourSum > 9 ? tempHourSum : "0"+tempHourSum ) +":"+ (tempMinSum>9 ? tempMinSum : "0"+tempMinSum) ); 
							}
							
							
							for(int i=0;i<yesterDayNewLineArr.length;i++){
								String[] tempArr = yesterDayNewLineArr[i].split(" "); // 데이터 각각으로 나눔
								for(int j=0; j<targetTimeArr.size();j++){
									for(int k=0;k<tempArr.length;k++){
									
										if(targetTimeArr.get(j).equals(tempArr[k])){
											String[] dataTimeHourSplit = tempArr[k].split(":");
											
											crawlingDataMap.put("lastCheckTime", yesterDaySplit[0]+"년 "+yesterDaySplit[1]+"월 "+yesterDaySplit[2]+"일 "+targetTimeArr.get(j) );
											crawlingDataMap.put("targetDate", yesterday);								// 어제 날짜
											crawlingDataMap.put("targetTime", dataTimeHourSplit[0]+":"+dataTimeHourSplit[1]);	// 데이터 시간
											crawlingDataMap.put("targetHour", dataTimeHourSplit[0]);
											crawlingDataMap.put("targetMin", dataTimeHourSplit[1]);											
											crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
											crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
											crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
											crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
											crawlingDataMap.put("co2", tempArr[k+5]);
											crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
											crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);	
											
											eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
										}
									}
									
								}
							}							
						}
						
					}else{ // 전날 데이터가 아예 없을경우
						
						// 24시를 분으로 환산하여 DB마지막 체크시간을 분으로 환산한 값을 빼어 차이값을 만든후 빈 횟수를 만듬
						int yesterDayTick = (24*60) /15;
						
						int tempHourSum = 0; // 24시까지 채울 임시 시간 변수
						int tempMinSum = 0;  // 24시까지 채울 임시 분 변수
						
						for(int i=0;i<yesterDayTick;i++){
							if( (tempMinSum+15) >= 60 ){ // 분을 기준으로 이번 틱에 60분이 넘는지 확인
								tempMinSum = 0;
								tempHourSum++;							
							}else{
								tempMinSum+=15;
							}
							targetTimeArr.add( (tempHourSum > 9 ? tempHourSum : "0"+tempHourSum ) +":"+ (tempMinSum>9 ? tempMinSum : "0"+tempMinSum) ); 
						}
						
						
						for(int i=0;i<yesterDayNewLineArr.length;i++){
							String[] tempArr = yesterDayNewLineArr[i].split(" "); // 데이터 각각으로 나눔
							for(int j=0; j<targetTimeArr.size();j++){
								for(int k=0;k<tempArr.length;k++){
								
									if(targetTimeArr.get(j).equals(tempArr[k])){
										String[] dataTimeHourSplit = tempArr[k].split(":");
																				
										crawlingDataMap.put("lastCheckTime", yesterDaySplit[0]+"년 "+yesterDaySplit[1]+"월 "+yesterDaySplit[2]+"일 "+targetTimeArr.get(j) ); // 임의의 최종 검침 날짜를 만들어줌
										crawlingDataMap.put("targetDate", yesterday);										// 어제 날짜
										crawlingDataMap.put("targetTime", dataTimeHourSplit[0]+":"+dataTimeHourSplit[1]);	// 데이터 시간
										crawlingDataMap.put("targetHour", dataTimeHourSplit[0]);
										crawlingDataMap.put("targetMin", dataTimeHourSplit[1]);											
										crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
										crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
										crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
										crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
										crawlingDataMap.put("co2", tempArr[k+5]);
										crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
										crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);	
										
										eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
									}
								}
								
							}
						}
					}
					
					// ====================================전날 데이터 세팅 E==========================================

					// ====================================금일 데이터 세팅 S==========================================
					targetTimeArr.clear();

					if((df.format(today).toString()+"일").equals(lastCheckTime[2])){
											
						int tempHourSum = 0;
						int tempMinSum = 0;
						
						tick = ((Integer.parseInt(webLastCheckHourMinArr[0])*60) + Integer.parseInt(webLastCheckHourMinArr[1])) / 15;
						
						for(int i=0;i<tick;i++){
							if( (tempMinSum+15) >= 60 ){ // 분을 기준으로 이번 틱에 60분이 넘는지 확인
								tempMinSum = 0;
								tempHourSum++;							
							}else{
								tempMinSum+=15;
							}
							targetTimeArr.add( (tempHourSum > 9 ? tempHourSum : "0"+tempHourSum ) +":"+ (tempMinSum>9 ? tempMinSum : "0"+tempMinSum) ); 
						}
											
						for(int j=0;j<newLineArr.length;j++){
							String[] tempArr = newLineArr[j].split(" "); // 데이터 각각으로 나눔
							for(int z=0; z<targetTimeArr.size();z++){
								for(int k=0;k<tempArr.length;k++){
								
									if(targetTimeArr.get(z).equals(tempArr[k])){
										String[] dataTimeHourSplit = tempArr[k].split(":");
										
										crawlingDataMap.put("lastCheckTime", ""+lastCheckTime[0]+" "+lastCheckTime[1]+" "+lastCheckTime[2]+" "+lastCheckTime[4]);
										crawlingDataMap.put("targetDate", sdf.format(today));		// 금일 날짜
										crawlingDataMap.put("targetTime", dataTimeHourSplit[0]+":"+dataTimeHourSplit[1]);				// 데이터 시간
										crawlingDataMap.put("targetHour", dataTimeHourSplit[0]);
										crawlingDataMap.put("targetMin", dataTimeHourSplit[1]);											
										crawlingDataMap.put("useKwh", removeComma(tempArr[k+1]));
										crawlingDataMap.put("maxUseKwh", removeComma(tempArr[k+2]));
										crawlingDataMap.put("lagReactPower", removeComma(tempArr[k+3]));
										crawlingDataMap.put("leadReactPower", removeComma(tempArr[k+4]));
										crawlingDataMap.put("co2", tempArr[k+5]);
										crawlingDataMap.put("lagPowerFactor", tempArr[k+6]);
										crawlingDataMap.put("leadPowerFactor", tempArr[k+7]);	
										
										eventDAO.powerPlanerUsageCrawling(crawlingDataMap);
									}
								}
								
							}
						}
					}
					
				}
									
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
