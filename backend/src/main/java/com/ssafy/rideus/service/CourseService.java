package com.ssafy.rideus.service;

import com.ssafy.rideus.common.api.S3Upload;
import com.ssafy.rideus.domain.Course;
import com.ssafy.rideus.dto.course.common.RecommendationCourseDto;
import com.ssafy.rideus.dto.course.common.RecommendationCourseDtoInterface;
import com.ssafy.rideus.dto.course.response.PopularityCourseResponse;
import com.ssafy.rideus.dto.coursecoordinate.CourseCheckpointDto;
import com.ssafy.rideus.dto.coursecoordinate.CourseCoordinateDto;
import com.ssafy.rideus.dto.review.ReviewStarAvgDto;
import com.ssafy.rideus.dto.review.ReviewStarAvgDtoInterface;
import com.ssafy.rideus.dto.tag.common.TagDto;
import com.ssafy.rideus.repository.jpa.CourseRepository;
import com.ssafy.rideus.repository.jpa.MemberTagRepository;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ssafy.rideus.common.exception.NotFoundException;
import com.ssafy.rideus.dto.course.common.CourseReviewTagTop5DtoInterface;
import com.ssafy.rideus.dto.course.common.RecommendationCourseDto;
import com.ssafy.rideus.dto.course.common.RecommendationCourseDtoInterface;
import com.ssafy.rideus.domain.Course;
import com.ssafy.rideus.domain.Member;
import com.ssafy.rideus.domain.Record;
import com.ssafy.rideus.domain.base.Coordinate;
import com.ssafy.rideus.domain.collection.CourseCoordinate;
import com.ssafy.rideus.repository.jpa.CourseRepository;
import com.ssafy.rideus.repository.jpa.MemberRepository;
import com.ssafy.rideus.repository.mongo.CourseCoordinateRepository;
import com.ssafy.rideus.repository.mongo.MongoRecordRepository;
import com.ssafy.rideus.repository.jpa.MemberTagRepository;
import com.ssafy.rideus.repository.jpa.RecordRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseService {
	
	private static final String SUCCESS = "success";
	private static final String FAIL = "fail";
	
	private final CourseCoordinateRepository courseCoordinateRepository;
	private final MongoRecordRepository mongoRecordRepository;
    private final CourseRepository courseRepository;
    private final RecordRepository recordRepository;
    private final MemberRepository memberRepository;
    private final MemberTagRepository memberTagRepository;
    
    private final MongoTemplate mongoTemplate;


	private final S3Upload s3Upload;
    private static List<Coordinate> checkpoints = new ArrayList<>();
	
	
    public List<RecommendationCourseDto> getRecommendationCourseByTag(Long memberId) {
        List<RecommendationCourseDtoInterface> recommendationCourseByMemberId = courseRepository.getRecommendationCourseByMemberId(memberId);

        List<RecommendationCourseDto> recommendationCourseDtos = new ArrayList<>();
        for (RecommendationCourseDtoInterface r : recommendationCourseByMemberId) {

            if (recommendationCourseDtos.isEmpty()) {
                recommendationCourseDtos.add(RecommendationCourseDto.from(r));
            } else {
                RecommendationCourseDto lastCourse = recommendationCourseDtos.get(recommendationCourseDtos.size() - 1);
                if(lastCourse.getCourseId().equals(r.getCourseId())) {
                    lastCourse.addTags(r.getTagId(), r.getTagName());
                } else {
                    recommendationCourseDtos.add(RecommendationCourseDto.from(r));
                }
            }

        }

//        System.out.println(recommendationCourseDtos.size());
//        System.out.println(recommendationCourseDtos);

        return recommendationCourseDtos;
    }


	public List<RecommendationCourseDto> getPopularityCourseWithBookmark(Long memberId) {
		List<RecommendationCourseDtoInterface> popularityCourses;
		if (memberId == 0) {
			popularityCourses = courseRepository.findAllOrderByLikeCountWithoutBookmark();
		} else {
			popularityCourses = courseRepository.findAllOrderByLikeCountWithBookmark(memberId);
		}

		List<RecommendationCourseDto> recommendationCourseDtos = new ArrayList<>();

		for (RecommendationCourseDtoInterface c : popularityCourses) {
			if (recommendationCourseDtos.isEmpty()) {
				recommendationCourseDtos.add(RecommendationCourseDto.from(c));
			} else {
				RecommendationCourseDto lastCourse = recommendationCourseDtos.get(recommendationCourseDtos.size() - 1);
				if(lastCourse.getCourseId().equals(c.getCourseId())) {
					lastCourse.addTags(c.getTagId(), c.getTagName());
				} else {
					recommendationCourseDtos.add(RecommendationCourseDto.from(c));
				}
			}
		}

		recommendationCourseDtos = recommendationCourseDtos.subList(0, 5);
		List<String> courseIds = new ArrayList<>();
		for (RecommendationCourseDto recommendationCourseDto : recommendationCourseDtos) {
			courseIds.add(recommendationCourseDto.getCourseId());
		}

		List<ReviewStarAvgDtoInterface> starAvgDtoInterfaceList = courseRepository.getCoursesStarAvg(courseIds);
		List<ReviewStarAvgDto> starAvgList = new ArrayList<ReviewStarAvgDto>();
		for(ReviewStarAvgDtoInterface r : starAvgDtoInterfaceList) {
			starAvgList.add(ReviewStarAvgDto.from(r));
		}

		// ?????? ?????? ?????? return ?????? ???????????????
		double starAvg;
		for(int i=0; i < recommendationCourseDtos.size(); i++) {
			starAvg = 0;
			RecommendationCourseDto course = recommendationCourseDtos.get(i);
			String courseId = course.getCourseId();
			for(int idx=0; idx<starAvgList.size(); idx++) {
				ReviewStarAvgDto starAvgDto = starAvgList.get(idx);
				if(courseId.equals(starAvgDto.getCourseId())) {
					starAvg = Math.round((starAvgDto.getSum() / starAvgDto.getCount())*10) / 10.0;
					break;
				}
			}

			recommendationCourseDtos.get(i).setStarAvg(starAvg);
		}

		Collections.sort(recommendationCourseDtos, new Comparator<RecommendationCourseDto>() {
			@Override
			public int compare(RecommendationCourseDto o1, RecommendationCourseDto o2) {
				return o2.getStarAvg().compareTo(o1.getStarAvg());
			}
		});

		return recommendationCourseDtos;
	}

//	public List<PopularityCourseResponse> getPopularityCourseWithBookmarkWithoutBookmark() {
//		List<Course> popularityCourses = courseRepository.findAllOrderByLikeCountWithoutBookmark();
//
//		return popularityCourses.stream().map(course -> PopularityCourseResponse.from(course)).collect(Collectors.toList());
//	}
	

    
	// ????????? ?????? ????????? ??????
    private Map<String, List<TagDto>> getAllCourseTagsMap(List<String> courseIds) {
    	List<CourseReviewTagTop5DtoInterface> allCourseTags;
    	if(courseIds.size() == 0) {
    		allCourseTags = courseRepository.getAllCourseTags();
    	} else {
    		allCourseTags = courseRepository.getSpecificCourseTags(courseIds);
    	}
        
        Map<String, List<TagDto>> allCourseTagsMap = new HashMap<String, List<TagDto>>();
        for(CourseReviewTagTop5DtoInterface courseTags : allCourseTags) {
        	String courseId = courseTags.getCourseId();
        	if(allCourseTagsMap.containsKey(courseId)) {
        		List<TagDto> tagList = allCourseTagsMap.get(courseId);
        		tagList.add(TagDto.from(courseTags.getTagId(), courseTags.getTagName()));
        		allCourseTagsMap.replace(courseId, tagList);
        	} else {
        		List<TagDto> tagList = new ArrayList<TagDto>();
        		tagList.add(TagDto.from(courseTags.getTagId(), courseTags.getTagName()));
        		allCourseTagsMap.put(courseTags.getCourseId(), tagList);
        	}
        }
        
        return allCourseTagsMap;
    }

	
	// ?????? ????????? ??????
    public List<RecommendationCourseDto> getAllCourses(Long memberId) {
    	// ?????? ?????? 
    	List<RecommendationCourseDtoInterface> allCourses = courseRepository.getAllCourses(memberId);
    	
    	// ????????? ?????? ????????? 
    	Map<String, List<TagDto>> allCourseTagsMap = getAllCourseTagsMap(new ArrayList<String>());
    	
    	// ?????? ?????? ?????? ??????
        List<RecommendationCourseDto> recommendationCourseDtoList = new ArrayList<>();
        for (RecommendationCourseDtoInterface course : allCourses) {
        	recommendationCourseDtoList.add(RecommendationCourseDto.find(course, allCourseTagsMap.get(course.getCourseId())));
        }

        return recommendationCourseDtoList;
    }
    
	
    // ?????? ?????? ?????? ??????
	public List<RecommendationCourseDto> getSpecificCourse(Long memberId, List<String> courseIds) {
		// ?????? ??????
		List<RecommendationCourseDtoInterface> courses = courseRepository.getSpecificCourse(memberId, courseIds);

		
    	// ????????? ?????? ????????? 
    	Map<String, List<TagDto>> allCourseTagsMap = getAllCourseTagsMap(courseIds);
		

//		CourseCoordinate courseDetail = courseCoordinateRepository.findById(courseId)
//				.orElseThrow(() -> new NotFoundException("?????? ?????? ?????? ??????"));
//		// CourseCoordinate courseDetail = courseCoordinateRepository.findById(coureId).get();
		
        List<RecommendationCourseDto> recommendationCourseDtoList = new ArrayList<>();
        for (RecommendationCourseDtoInterface course : courses) {
        	recommendationCourseDtoList.add(RecommendationCourseDto.find(course, allCourseTagsMap.get(course.getCourseId())));
        }

        return recommendationCourseDtoList;
	}
	
	
	// RecommendationCourseDto -> Map (Dto + ?????? ??????)
	private Map<String, Object> convertCourseDtoIntoMap(RecommendationCourseDto course, Double starAvg) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		// Map??? ??????
		resultMap.put("courseId", course.getCourseId());
		resultMap.put("courseName", course.getCourseName());
		resultMap.put("distance", course.getDistance());
		resultMap.put("expectedTime", course.getExpectedTime());
		resultMap.put("start", course.getStart());
		resultMap.put("finish", course.getFinish());
		resultMap.put("likeCount", course.getLikeCount());
		resultMap.put("imageUrl", course.getImageUrl());
		resultMap.put("category", course.getCategory());
		resultMap.put("bookmarkId", course.getBookmarkId());
		resultMap.put("starAvg", starAvg);
		resultMap.put("tags", course.getTags());
		
		return resultMap;
	}
	
	
	
    // ?????? ?????? ??????
	// ?????? ?????? ?????? ?????? + ???????????? ????????? ????????????
	public Map<String, Object> getCourse(Long memberId, String courseId) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		// MySQL??? ?????? ?????? ?????????
		List<String> courseIds = new ArrayList<String>();
		courseIds.add(courseId);
		RecommendationCourseDto course = getSpecificCourse(memberId, courseIds).get(0);
		
		// ?????? ????????? ?????? ?????? ??????(count, sum) ?????? ????????????
		List<ReviewStarAvgDtoInterface> starAvgDtoInterfaceList = courseRepository.getCoursesStarAvg(courseIds);
		
		// ?????? ?????? ??????
		double starAvg;
		if(starAvgDtoInterfaceList.size() > 0) {
			ReviewStarAvgDto starAvgDto = ReviewStarAvgDto.from(starAvgDtoInterfaceList.get(0));
			starAvg = Math.round((starAvgDto.getSum() / starAvgDto.getCount())*10) / 10.0;
		} else {
			starAvg = 0;
		}
		
		
		// MongoDB??? ?????? ????????? ????????????
		CourseCoordinateDto courseCoordinate = mongoTemplate.findById(courseId, CourseCoordinateDto.class, "course_coordinate");
		
		// Map??? ??????
		resultMap = convertCourseDtoIntoMap(course, starAvg);
		resultMap.put("coordinates", courseCoordinate.getCoordinates());
		resultMap.put("checkpoints", courseCoordinate.getCheckpoints());
		
		
		return resultMap;
	}
	
	
	// ?????? ??????
	public List<RecommendationCourseDto> getAllCoursesByKeyword(Long memberId, String keyword) {
		
		Set<String> courseIdsSet = new HashSet<String>();
		
		List<String> courseIds1 = courseRepository.getAllCourseIdsByKeyword(keyword);
		for(int i=0; i<courseIds1.size(); i++) {
			courseIdsSet.add(courseIds1.get(i));
		}
		List<String> courseIds2 = courseRepository.getAllCourseIdsByTagKeyword(keyword);
		for(int i=0; i<courseIds2.size(); i++) {
			courseIdsSet.add(courseIds2.get(i));
		}		
		
		List<String> courseIds = new ArrayList<String>(courseIdsSet);
//		System.out.println(courseIds.toString());
		
		return getSpecificCourse(memberId, courseIds);
	}
	

	// ??? ?????? ?????? ?????? ??????
	public List<Map<String, Object>> getAllCoursesByLoc(Long memberId, Double lat, Double lng) {
		
		List<Map<String, Object>> resultMapList = new ArrayList<Map<String,Object>>();
		
//		List<CourseCoordinate> allCourseCoordinates = courseCoordinateRepository.findAll();
		List<CourseCheckpointDto> allCourseCheckpoints = mongoTemplate.findAll(CourseCheckpointDto.class, "course_coordinate");
		
		Map<String, Double> intervalMap = new HashMap<String, Double>();
		double lat1 = lat;
		double lon1 = lng;
		for(CourseCheckpointDto courseCheckpoint : allCourseCheckpoints) {
			double lat2 = Double.parseDouble(courseCheckpoint.getCheckpoints().get(0).getLat());
			double lon2 = Double.parseDouble(courseCheckpoint.getCheckpoints().get(0).getLng());
			intervalMap.put(courseCheckpoint.getId(), intervalMeter(lat1, lon1, lat2, lon2));
		}
		
		// intervalMeter ?????? ???????????? ??????
		
		// Map.Entry ????????? ??????
		List<Entry<String, Double>> listEntries = new ArrayList<Entry<String, Double>>(intervalMap.entrySet());

		// ???????????? Comparator??? ???????????? ?????????????????? ??????
		Collections.sort(listEntries, new Comparator<Entry<String, Double>>() {
			// compare??? ?????? ??????
			public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2) {
				// ?????? ?????? ??????
				return obj1.getValue().compareTo(obj2.getValue());
			}
		});

//		System.out.println("?????? ?????? ?????? ?????? ??????");
//		// ?????? ??????
//		for(Entry<String, Double> entry : listEntries) {
//			System.out.println(entry.getKey() + " : " + entry.getValue());
//		}
//		System.out.println("_______?????? ?????? ???_________");
		
		// ????????? ?????? ?????? ?????? ?????????????????? ????????? ???????????????
		List<String> courseIds = new ArrayList<String>();
		// ????????? ?????? ??????
		int courseCnt = 5;
		for(int i=0; i<courseCnt; i++) {
			courseIds.add(listEntries.get(i).getKey());
		}

		List<ReviewStarAvgDtoInterface> starAvgDtoInterfaceList = courseRepository.getCoursesStarAvg(courseIds);
		List<ReviewStarAvgDto> starAvgList = new ArrayList<ReviewStarAvgDto>();
		for(ReviewStarAvgDtoInterface r : starAvgDtoInterfaceList) {
			starAvgList.add(ReviewStarAvgDto.from(r));
		}
		
		
		// DB?????? ????????? ?????? ?????? ?????????
		List<RecommendationCourseDto> courseList = getSpecificCourse(memberId, courseIds);
		List<RecommendationCourseDto> sortedCourseList = new ArrayList<RecommendationCourseDto>();
		for(int i=0; i<courseCnt; i++) {
			String courseId = courseIds.get(i);
			for(int clIdx=0; clIdx<courseCnt; clIdx++) {
				RecommendationCourseDto course = courseList.get(clIdx);
				if(courseId.equals(course.getCourseId())) {
					sortedCourseList.add(course);
					break;
				}
			}
		}
//		System.out.println(">>>courseIds.toString() "+courseIds.toString());
//		System.out.println(">>>sortedCourseList.toString() "+sortedCourseList.toString());
		
		
		// ?????? ?????? ?????? return ?????? ???????????????
		double starAvg;
		for(int i=0; i<courseCnt; i++) {
			starAvg = 0;
			RecommendationCourseDto course = sortedCourseList.get(i);
			String courseId = course.getCourseId();
			for(int idx=0; idx<starAvgList.size(); idx++) {
				ReviewStarAvgDto starAvgDto = starAvgList.get(idx);
				if(courseId.equals(starAvgDto.getCourseId())) {
					starAvg = Math.round((starAvgDto.getSum() / starAvgDto.getCount())*10) / 10.0;
					break;
				}
			}
			
			resultMapList.add(convertCourseDtoIntoMap(course, starAvg));
		}
		
		return resultMapList;
	}
	
	
	// ?????? ??????
	@Transactional
	public String addCourseData(Map<String, String> inputMap, Long memberId, MultipartFile image) {
		// inputMap - memberId, courseName, distance, recordId

		// ?????? ???????????? ????????? ????????? ???????????? ??????
		
		
		try {
			// MongoDB??? ????????? ??????
			
			// ?????? ????????? ????????? ?????? ?????? ????????? ?????? ????????? ??????
			String recordId = inputMap.get("recordId");
			Record originRecord = recordRepository.findById(recordId).get();
			// ?????? ?????? ?????????
			List<Coordinate> coordinates = new ArrayList<Coordinate>();
			coordinates = mongoRecordRepository.findById(recordId).get().getCoordinates();
			
			// ??????????????? ?????? ????????? ??????
			List<Coordinate> checkpoints = getCheckpoints(coordinates);
			
			// ?????? ??????, ??????????????? ?????? ????????? MongoDB??? ??????
			CourseCoordinate courseCoordinate = courseCoordinateRepository.save(CourseCoordinate.create(coordinates, checkpoints));
			String courseId = courseCoordinate.getId();
			

			// MySQL??? ????????? ??????
			
	    	// (?????????(MongDB ???????????? ??????), ?????????, ?????? ??????, ?????? ??????, ?????? ??????, ?????? ?????? ??????, ????????? ??????, ?????? url, ????????????, ????????????????, ????????????)
	        String courseName = inputMap.get("courseName");
	        Double distance = originRecord.getRecordDistance();
	        String distanceStr = String.valueOf(distance);

	        Coordinate startCoordinate = checkpoints.get(0);
	        Coordinate finishCoordinate = checkpoints.get(checkpoints.size()-1);
			String start = locAPI(startCoordinate.getLng(), startCoordinate.getLat());
			String finish = locAPI(finishCoordinate.getLng(), finishCoordinate.getLat());
	        
			int expectedTime = Integer.parseInt(calExpectedTime(distanceStr));

			// ????????? s3??? ?????????
			String imageUrl = s3Upload.uploadImageToS3(image);


			// ?????? db ???????????? member_id ??? ????????????
			Member member = memberRepository.findById(memberId).get();
			Course course = new Course(courseId, courseName, distance, start, finish, expectedTime, 0, imageUrl, null, member, null, null);
//	        courseRepository.save(new Course(courseId, courseName, distance, start, finish, expectedTime, 0, null, null, null));
			Course saveCourse = courseRepository.save(course);

			// ????????? ????????? record??? course ?????? ????????? ??????(update)
	        Record record = Record.findCourse(originRecord, saveCourse);
	        recordRepository.save(record);
	        
	        return SUCCESS;
			
		} catch (Exception e) {
			System.out.println(">>> addCourseData() Exception: "+e);
			return FAIL;
		}
		
	}
	
	
	
	
	/////////////////////////////////////////////////////////////////////	
	// ?????? ?????? ????????? ????????? ??????
	@Transactional
	public void addCrawlingData() {
		
		try { 
			// 1. ?????? ?????????????????? ?????? ???, ?????? ?????? ????????? ???????????????
			String[][] additionalData = getAdditionalData();
			
			
			// 2. Gpx File ??????
			
//            import org.w3c.dom.Document;
//            import org.w3c.dom.Element;
//            import org.w3c.dom.Node;
//            import org.w3c.dom.NodeList;
//            import org.xml.sax.SAXException;
			
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        
	        String filePath = "C:\\Users\\SSAFY\\Desktop\\RideUs\\course-crawling\\gpx-files\\";
	        int fileName = 1;
	        // 136: ????????? ????????? ??????, 137~141 : ????????? ??????
	        int fileCount = 135;
	        String type = ".gpx";
	        
	        for(; fileName <= fileCount; fileName++) {
	        	
	            // .gpx ?????? ??????
	            File gpxFile = new File(filePath + fileName + type);
	            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	            org.w3c.dom.Document doc = dBuilder.parse(gpxFile);
	            
	            org.w3c.dom.Element root = doc.getDocumentElement();
	            Node firstNode = root.getFirstChild();
	            Node metadata = firstNode.getNextSibling();
	            NodeList childList = metadata.getChildNodes();
	            Node link = childList.item(3);
	            String courseURL = link.getAttributes().getNamedItem("href").getTextContent();
	            
            
	            
	            // trk tag ??????
	            NodeList trk = doc.getElementsByTagName("trk");
	
	            NodeList childNodes = doc.getChildNodes();
	
	            // metadata, trk tag
	            NodeList bigChilds = childNodes.item(0).getChildNodes();
	
	            Node trkNode = bigChilds.item(3); // trk tag
	
	            // name, trkseg tag
	            NodeList trkList = trkNode.getChildNodes();
	            // ?????? ????????? name ??? ??????
	            Node name = trkList.item(1);
	
	           
	            String[] trkName = name.getTextContent().split("- ");
	            String gpxCourseName = trkName[1].trim();
	            
	            NodeList trkptList = doc.getElementsByTagName("trkpt");
	            
	            
	            // ?????? ?????? ????????? ??????
	            List<Coordinate> coordinates = getCourseCoordinates(trkptList);
	            
	            // 7m ?????? ?????? ?????? ??????
//	            List<Coordinate> coor = addCourseCoordinates(coordinates);
	            
	            // ??????????????? ?????? ????????? ??????
	            getCheckpoints(coordinates);
	            

	            // ?????? ??????, ??????????????? ?????? ????????? MongoDB??? ??????
	            CourseCoordinate courseCoordinate = courseCoordinateRepository.save(CourseCoordinate.create(coordinates, checkpoints));
	            String courseId = courseCoordinate.getId();
          
	            // gpx ?????? ????????? ???????????? ???????????? ?????? ?????? ??????    ?????? ????????? ????????? Course ?????? ?????? ??? ??????
	            String[] courseAdditionalData = searchCourse(courseURL, additionalData);
//	            System.out.println("courseName: "+gpxCourseName+" // distanceInfo: "+courseAdditionalData[2]);
	            
	            
	            // ?????? ?????? ?????? ?????? MySQL??? ??????
	        	// (?????????(MongDB ???????????? ??????), ?????????, ?????? ??????, ?????? ??????, ?????? ??????, ?????? ?????? ??????, ????????? ??????, ?????? url, ????????????)
	            String courseName = gpxCourseName;
	            double distance = Math.round(Double.parseDouble(courseAdditionalData[2])*100)/100.0;

	            Coordinate startCoordinate = checkpoints.get(0);
	            Coordinate finishCoordinate = checkpoints.get(checkpoints.size()-1);
	    		String start = locAPI(startCoordinate.getLng(), startCoordinate.getLat());
	    		String finish = locAPI(finishCoordinate.getLng(), finishCoordinate.getLat());
	            
	    		checkpoints.clear();
	    		int expectedTime = Integer.parseInt(courseAdditionalData[3]);
	            
	            // ?????? db ???????????? member_id ??? ????????????
//	            courseRepository.save(new Course(courseId, courseName, distance, start, finish, expectedTime, 0, null, null, null));
	            courseRepository.save(new Course(courseId, courseName, distance, start, finish, expectedTime, 0, null, null, null, null, null));
	        }
	            
	            
		} catch (Exception e) {
			System.out.println(">>> addCrawlingData() Exception: "+e);
		}
	}
	
	
	// ?????? ?????????????????? ?????????, ?????? ?????? ????????? + ?????? ????????? ?????? ?????? ?????? ??????
	private String[][] getAdditionalData() {
		// ?????? ?????? ?????? (gpx ?????? ??????)
		int fileCount = 141;

		// 0: ?????? ??????, 1: ?????? ???, 2: ?????? ??????, 3: ?????? ?????? ??????
		String[][] courseInfoArr = new String[fileCount][4];
		String[] city = {"seoul", "busan", "gyeonggi", "daejeon"};
		
		int idx = 0;
		
		for(int i=0; i<4; i++) {
			String url = "https://www.trekbikes.com/kr/ko_KR/greatrides/"+city[i];
			Document doc;
			try {
				doc = Jsoup.connect(url).header("Content-Type", "application/json;charset=UTF-8").get();
				Elements courseBlocks = doc.select("div.editorial-small__content");
				
				for( Element courseBlock : courseBlocks ) {
					String courseURL = ""; // ?????? ??????
					String courseTitle = ""; // ?????????
					String[] courseInfo = null; // ?????? ??????
					String courseDistance = ""; // ??????
					
					courseURL = courseBlock.select("div.editorial-small__content > a").attr("href");
					
					courseTitle = courseBlock.select("h4.editorial-small__header").text();
					// couseTitle ???????????? ???????????? ????????? ????????? ?????????
					if(courseTitle.contains("??????")) {
						break;
					}
					
					courseInfo = courseBlock.select("p.text-weak").text().split(" ");
					courseDistance = courseInfo[1];
//					System.out.println(">>> ?????????: "+courseTitle+" ?????? ??????: "+courseDistance);
					
					// 0: ?????? ??????, 1: ?????? ???, 2: ?????? ??????, 3: ?????? ?????? ??????
					courseInfoArr[idx][0] = courseURL;
					courseInfoArr[idx][1] = courseTitle;
					courseInfoArr[idx][2] = courseDistance;
					courseInfoArr[idx][3] = calExpectedTime(courseDistance);
					
					idx++;
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(">>> getAdditionalData() Exception: " + e);
			}
		}
		return courseInfoArr;		
	}
	
	
	// ?????? ?????? ??????(???) ????????? (distance: ?????? ??????, ?????? ?????? km, ?????? 25km)
	private static String calExpectedTime(String distance) {
		
		// 1??? ??????
		Double vel = 25.0 / 60.0;
		// ????????? ?????? ??????????????????
		Double v = Math.round(vel*100)/100.0;
		
		try {
			int expectedTime = (int) Math.ceil((Double.parseDouble(distance) / v));
			
			return Integer.toString(expectedTime);
		} catch(Exception e) {
			System.out.println(">>> calExpectedTime() Exception : "+e);
			return "";
		}

	}
	
	
	// gpx ???????????? ?????? ?????? ????????? ????????????
	private static List<Coordinate> getCourseCoordinates(NodeList coordinateList) {
		List<Coordinate> coordinates = new ArrayList<>();
		try {
			for(int index=0; index<coordinateList.getLength(); index++) {
            	
                Node node = coordinateList.item(index);

                Node lat = node.getAttributes().getNamedItem("lat");
                Node lon = node.getAttributes().getNamedItem("lon");

                String latstr = lat.getTextContent();
                String lonstr = lon.getTextContent();
                
                Coordinate coordinate = new Coordinate(latstr, lonstr);
                coordinates.add(coordinate);
            } // end of for

            return coordinates;
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(">>> getCourseCoordinates() Exception: "+e);
			return coordinates;
		}
	}

	
	// gpx ????????? ?????? ????????? ??????????????? ????????? ??????
	// ?????? ??? ????????? ?????? ?????? ??????(standard) ??? ????????? ?????? ????????? ?????????????????? ??????
	private static List<Coordinate> getCheckpoints(List<Coordinate> coordinateList) {
		
		
		// ?????? ????????? ?????????????????? ??????
		checkpoints.add(coordinateList.get(0));
		
		double distSum = 0;
		double dist;
		
		// ?????? ????????? ?????? ?????? ?????? 1000m(1km)
//		double standard = 1000;
		int listIdx = coordinateList.size()-1;
		for(int i=0; i<listIdx; i++) {
			Coordinate coordinate1 = coordinateList.get(i);
			Coordinate coordinate2 = coordinateList.get(i+1);
			
			dist = intervalMeter(Double.parseDouble(coordinate1.getLat()), Double.parseDouble(coordinate1.getLng()), Double.parseDouble(coordinate2.getLat()), Double.parseDouble(coordinate2.getLng()));
//			System.out.println(i+". dist: "+dist);
			distSum += dist;
//			System.out.println("distSum: "+distSum);
			
			// ?????? ??? ?????? ?????? standard?????? ????????? ??? ??? ????????? ?????????????????? ??????
			if(distSum > 1000) {
				checkpoints.add(coordinate1);
				// ?????????
				distSum = 0;
				i--;
			}
		}
		
		// ?????? ????????? ?????????????????? ??????
		checkpoints.add(coordinateList.get(listIdx));
//		System.out.println(checkpoints.toString());
        
        return checkpoints;
	}
	
	
	// ??? ?????? ??? ?????? (??????: meter)
    public static double intervalMeter(double lat1, double lon1, double lat2, double lon2) {
        
    	if(lat1 == lat2 && lon1 == lon2) {
    		return 0;
    	}
    	
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
         
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        
        // dist ?????? meter??? ?????????
        dist = dist * 1609.344;
        
        // ????????? ?????????????????? ???????????? ??????
        // ?????? 4km??? ??????????????? ?????? ??? ???????????? ?????? ?????? ?????????????????? ?????? ??????(floor)???
        dist = Math.floor(dist*100)/100.0;
        
        return dist;
    }
     
    // This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
     
    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
	

	// kakao api ???????????? ????????? ?????? ??????(~???) ?????? (?????? ??????, ?????? ??????  ????????? ????????? ??? ??????)
	private static String locAPI(String lng, String lat) throws URISyntaxException {
		
		String addressName = "";
		
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
        // REST_API_KEY 
		final String restApiKey = "KakaoAK b834a23c18a62707b7f97e56c265b1ea";
		headers.set("Authorization", restApiKey);
		
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
		// x: ?????? longitude, y: ?????? latitude
		String apiURL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x="+lng+"&y="+lat;
		URI uri = new URI(apiURL);             
		
		ResponseEntity<String> res = rest.exchange(uri, HttpMethod.GET, entity, String.class);
		
		try {
			JSONObject locJsonObj1 = new JSONObject(res.getBody());
			JSONArray locJsonArr = new JSONArray(locJsonObj1.getString("documents"));
			JSONObject locJsonObj2 = (JSONObject) locJsonArr.get(0);
			addressName = locJsonObj2.getString("address_name");
			
			return addressName;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(">>> locAPI() Exception: "+e);
			return addressName;
		}
	}
	

	// gpx ?????? ???????????? ?????? ???????????? ????????? ????????? ??????
    public static String[] searchCourse(String courseURL, String[][] additionalData) {
    	int fileCount = additionalData.length;
    	for(int i=0; i<fileCount; i++) {
    		if(additionalData[i][0].equals(courseURL)) {
    			return additionalData[i];
    		}
    	}
    	System.out.println("?????? ????????? ?????? ??? ???");
    	return null;
    }	

}
