import styled from "styled-components";
import {StyledText} from "./Common";
import {Box} from "grommet";
import {Notes} from "grommet-icons";
import {expectTimeHandle} from "../utils/util";

const CourseBox = styled.div`
  flex-direction: column;
  display: flex;
  justify-content: space-between;
  width: 130px;
  height: 130px;
  margin-right: 15px;
  margin-bottom: 10px;
  margin-top: 5px;

  padding: 10px;
  padding-top: 14px;
  background-color: white;
  border-radius: 15px;
  box-shadow: 4px 4px 4px -4px rgb(0 0 0 / 0.2);
`;

const Wrap = styled.div`
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: normal;
  word-break: break-word;

  display: -webkit-box;
  -webkit-line-clamp: 2; // 원하는 라인수
  -webkit-box-orient: vertical;
`;

export const MyPageCourse = ({course}) => {
    return (
        <CourseBox>
            <div style={{display: "flex", flexDirection: "column"}}>
                <div style={{display: "flex", justifyContent: "flex-start"}}>
                    <Wrap>
                        <StyledText
                            text={course?.courseName}
                            weight="bold" color="#000000"

                        />
                    </Wrap>
                </div>

                <Box align="end" gap="small" margin={{top: "2vw"}}>
                    <Notes/>
                </Box>
            </div>
            <div
                align="end" gap="small" margin={{top: "25px"}}
            >

                <Box align={"end"}>
                    <StyledText
                        text={course?.distance + "km 주행"}
                        style={{
                            backgroundColor: "#BDE0EF",
                            borderRadius: "10px",
                            // margin: "3px",
                            fontSize: "12px",
                            paddingLeft: "10px",
                            paddingRight: "10px",
                        }}
                        weight="bold"
                    />
                    <StyledText
                        text={expectTimeHandle(course?.expectedTime) + " 주행"}
                        style={{
                            backgroundColor: "#F8F38F",
                            borderRadius: "10px",
                            fontSize: "12px",
                            paddingLeft: "10px",
                            paddingRight: "10px",
                        }}
                        weight="bold"
                    />
                    <StyledText
                        text={
                            course.startedLocation && course?.startedLocation.split(" ")[0] + " " + course?.startedLocation.split(" ")[1]
                        }
                        style={{
                            backgroundColor: "#F4D4D4",
                            borderRadius: "10px",
                            fontSize: "12px",
                            paddingLeft: "10px",
                            paddingRight: "10px",
                        }}
                        weight="bold"
                    />
                    {/*<StyledText text={`${course?.distance}km`} />*/}
                    {/*<StyledText*/}
                    {/*  text={`${parseInt(course?.expectedTime / 60)}h ${*/}
                    {/*    course?.expectedTime % 60*/}
                    {/*  }m`}*/}
                    {/*/>*/}
                </Box>
            </div>
        </CourseBox>
    );
};