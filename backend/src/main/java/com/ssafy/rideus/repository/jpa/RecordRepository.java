package com.ssafy.rideus.repository.jpa;

import com.ssafy.rideus.domain.Record;
import com.ssafy.rideus.dto.rank.response.*;
import com.ssafy.rideus.dto.record.response.RecordTotalResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecordRepository extends JpaRepository<Record, String> {
    public List<Record> findTop5RecordsByMemberIdOrderByIdDesc(Long id);

    @Query(value = "SELECT m.member_id as memberId, m.nickname, m.profile_image_url as profileImageUrl, speedBest, RANK() OVER (ORDER BY speedBest DESC) AS ranking\n" +
            "from (select member_id, max(record_speed_best) speedBest from record r group by r.member_id) r join member m on r.member_id = m.member_id;", nativeQuery = true)
    List<RankBestSpeedResponseDtoInterface> searchRankTotalBestSpeed();

    @Query(value = "SELECT m.member_id as memberId, m.nickname, m.profile_image_url as profileImageUrl, min timeMinute, RANK() OVER (ORDER BY min ASC) AS ranking\n" +
            "from (select member_id, min(record_time_minute) as min\n" +
            "from record\n" +
            "where course_id = :courseId and record_is_finished = true\n" +
            "group by member_id) r join member m on r.member_id = m.member_id", nativeQuery = true)
    List<RankCourseTimeResponseDtoInterface> searchRankCourseTime(@Param("courseId") String courseId);

    @Query(value = "SELECT m.member_id as memberId, m.nickname, m.profile_image_url as profileImageUrl, r.record_speed_best as speedBest, " +
            "RANK() OVER (ORDER BY r.record_speed_best DESC) AS ranking FROM record r join member m on r.member_id = m.member_id LIMIT 3", nativeQuery = true)
    List<RankBestSpeedResponseDtoInterface> searchRankTotalBestSpeedTop3();

    @Query(value = "SELECT a.ranking " +
            "FROM (SELECT m.member_id, " +
            "RANK() OVER (ORDER BY r.record_speed_best DESC) AS ranking FROM record r JOIN member m ON r.member_id = m.member_id) a " +
            "WHERE a.member_id = :memberId ", nativeQuery = true)
    Long searchMyRankTotalBestSpeed(@Param("memberId") Long memberId);

    @Query(value = "SELECT * " +
            "FROM (SELECT m.member_id AS memberId, m.nickname, m.profile_image_url AS profileImageUrl, RANK() OVER (ORDER BY r.record_speed_best DESC) AS ranking " +
            "FROM record r JOIN member m ON r.member_id = m.member_id) c " +
            "WHERE ranking BETWEEN :myBestSpeedRank -1 AND :myBestSpeedRank + 1", nativeQuery = true)
    List<RankBestSpeedResponseDtoInterface> searchRankMemberBestSpeedWithUpAndDown(@Param("myBestSpeedRank") Long myBestSpeedRank);

    @Query("select r from Record r left outer join fetch r.course c join fetch r.member join fetch r.rideRoom where r.id = :recordId")
    Optional<Record> findRecordWithCourseAndRideRoomAndMember(String recordId);

    @Query("select r from Record r where r.member.id = :memberId and r.recordIsMine = true order by r.createdDate desc")
    List<Record> findMyRideRecentRecord(Long memberId);

    List<Record> findRecordByRideRoomIdOrderByRecordDistanceDesc(Long roomId);

    @Query(value = "SELECT m.member_id as memberId, m.nickname, m.profile_image_url as profileImageUrl, totalTime, RANK() OVER (ORDER BY totalTime DESC) AS ranking\n" +
            "from (select member_id, sum(record_time) totalTime from record r where created_date BETWEEN DATE_ADD(NOW(), INTERVAL -1 MONTH ) AND NOW() group by r.member_id ) r join member m on r.member_id = m.member_id;", nativeQuery = true)
    List<RankTimeResponseDtoInterface> searchRankTotalTime();

    @Query(value = "SELECT m.member_id as memberId, m.nickname, m.profile_image_url as profileImageUrl, totalDistance, RANK() OVER (ORDER BY totalDistance DESC) AS ranking\n" +
            "from (select member_id, sum(record_distance) totalDistance from record r where created_date BETWEEN DATE_ADD(NOW(), INTERVAL -1 MONTH ) AND NOW() group by r.member_id ) r join member m on r.member_id = m.member_id;", nativeQuery = true)
    List<RankDistanceResponseDtoInterface> searchRankTotalDistance();
}
