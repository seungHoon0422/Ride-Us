import { Box, Grommet, Notification } from "grommet";
import React, { useEffect, useState } from "react";
import { Map, Polyline } from "react-kakao-maps-sdk";
import styled from "styled-components";
import CloseButton from "../assets/images/close.png";
import { StyledText } from "../components/Common";
import Star from "../assets/images/star_review.png";
import StarBlank from "../assets/images/star_review_blank.png";
import { useLocation, useNavigate } from "react-router-dom";
import ImInput from "../assets/icons/imageInput.svg";
import { FlexBox, STextArea } from "../components/UserStyled";
import Button from "../components/Button";
import {
  expectTimeHandle,
  tags as initTags,
  TimeBox,
  TimeBoxSecond,
} from "../utils/util";
import { BootstrapButton, RegisterButton } from "../components/Buttons";
import { AlertDialog } from "../components/AlertDialog";
import { writeReview } from "../utils/api/reviewApi";
import { ReactComponent as Fail } from "../assets/icons/fail.svg";
import Clock from "../assets/icons/clock.svg";
import Flag from "../assets/icons/flag.svg";
import { motion } from "framer-motion";

const HeaderDiv = styled.div`
  margin: 5px;
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid black;
  padding: 5px;
  padding-bottom: 10px;
`;

const BackButton = styled(motion.button)`
  background: none;
  font-size: 12px;
  font-family: Noto Sans KR, sans-serif;
  border: 0px;
  width: 10vw;
`;

export const ImageBtn = styled.img`
  background: #fff;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  &:hover {
    background: rgb(77, 77, 77);
    color: #fff;
  }
`;
//   courseData: {
// courseName: courseName,
//     latlng: mapData.latlng,
//     topSpeed: data.topSpeed,
//     avgSpeed: data.avgSpeed,
//     nowTime: nowTime,
//     totalDistance: data.totalDistance,
//   },

const GrommetTheme = {
  notification: {
    toast: {
      time: 600,
      container: {
        width: "small",
      },
    },
    normal: {
      toast: {
        background: {
          color: "white",
          opacity: "strong",
        },
      },
    },
    critical: {
      toast: {
        background: {
          color: "white",
          opacity: "strong",
        },
      },
      icon: () => <Fail />,
    },
  },
  carousel: {
    animation: {
      duration: 300,
    },
  },
};

const HeaderBox = ({ goBack }) => {
  return (
    <HeaderDiv>
      <div style={{ width: "10vw" }}></div>
      <StyledText
        size="20px"
        weight="bold"
        text="?????? ??????"
        style={{
          alignItems: "center",
        }}
      />
      <BackButton whileTap={{ scale: 1.2 }} onClick={goBack}>
        <img
          src={CloseButton}
          style={{
            alignItems: "center",
            display: "flex",
          }}
        />
      </BackButton>
    </HeaderDiv>
  );
};

export const ReviewRegister = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [reviewDesc, setReviewDesc] = useState("");
  const [tags, setTags] = useState([]);
  const [image, setImage] = useState();
  const [select, setSelect] = useState([]);
  const [clicked, setClicked] = useState([false, false, false, false, false]);
  const { courseData } = location.state;
  const [open, setOpen] = useState(false);
  const [notValid, setNotValid] = useState(false);
  const [exit, setExit] = useState(false);
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const handleStarClick = (index) => {
    let clickStates = [...clicked];
    for (let i = 0; i < 5; i++) {
      clickStates[i] = i <= index ? true : false;
    }
    setClicked(clickStates);
  };
  const array = [0, 1, 2, 3, 4];

  useEffect(() => {
    setTags(initTags);
  }, []);
  const handleImageUpload = (e) => {
    const fileArr = e.target.files;
    // console.log(fileArr[0]);
    setImage(fileArr[0]);
  };

  const handleText = (text) => {
    //  console.log(text);
    const MAX_LENGTH = 50;
    if (text.length <= MAX_LENGTH) setReviewDesc(text);
  };

  const isValied = () => {
    let score = clicked.filter(Boolean).length;
    const request = {
      recordId: courseData.recordId,
      score: score,
      content: reviewDesc,
      tags: select,
    };
    console.log(request);
    if (score === 0 || image === undefined || select.length === 0) return false;
    else return true;
  };

  const handleRegister = () => {
    let score = clicked.filter(Boolean).length;
    const request = {
      recordId: courseData.recordId,
      score: score,
      content: reviewDesc,
      tags: select,
    };
    console.log(request);
    const formData = new FormData();
    formData.append("image", image);
    const blob = new Blob([JSON.stringify(request)], {
      type: "application/json",
    });
    formData.append("reviewRequestDto", blob);
    writeReview(
      formData,
      (response) => {
        console.log(response);
        navigate(`/courseDetail/${courseData.courseId}`);
      },
      (fail) => {
        console.log(fail);
      }
    );
  };
  return (
    <Box>
      {/* ?????? ?????? */}
      <HeaderBox
        goBack={() => {
          setExit(true);
        }}
      />
      {/* ?????? */}
      <Box align="center" margin={{ top: "20px" }} gap="medium">
        {/* ??????, ?????? ??????, ????????? */}
        <Box direction="row" gap="large">
          {/* ???????????? */}
          <Map
            center={courseData.latlng[parseInt(courseData.latlng.length / 2)]}
            isPanto={true}
            level={4}
            style={{ borderRadius: "10px", width: "120px", height: "120px" }}
          >
            <Polyline
              path={[courseData.latlng]}
              strokeWeight={3} // ?????? ?????? ?????????
              strokeColor={"#030ff1"} // ?????? ???????????????
              strokeOpacity={0.7} // ?????? ???????????? ????????? 1?????? 0 ????????? ????????? 0??? ??????????????? ???????????????
              strokeStyle={"solid"} // ?????? ??????????????????
            />
          </Map>
          <Box gap="medium" justify="center">
            <StyledText
              text={courseData.courseName}
              size="18px"
              style={{
                fontFamily: "gwtt",
              }}
            />
            <Box
              direction="row"
              gap="medium"
              margin={{
                top: "10px",
                right: "15px",
              }}
            >
              <Box
                direction="row"
                align="center"
                gap="4px"
                style={{
                  backgroundColor: "#F8F38F",
                  borderRadius: "10px",
                  // margin: "3px",
                  padding: "4px 6px",
                }}
              >
                <img src={Clock} width="13px" height="13px" />
                <TimeBoxSecond time={courseData.nowTime} />
              </Box>
              <Box
                direction="row"
                align="center"
                gap="4px"
                style={{
                  backgroundColor: "#BDE0EF",
                  borderRadius: "10px",
                  // margin: "3px",
                  padding: "4px 6px",
                }}
              >
                <img src={Flag} width="13px" height="13px" />
                <Box align="end" direction="row">
                  <StyledText
                    text={courseData.totalDistance}
                    weight="bold"
                    size="17px"
                  />
                  <StyledText size="13px" text={"km"} weight="bold" />
                </Box>
              </Box>
            </Box>
          </Box>
        </Box>
        {/* ??????, ?????? ??????, ????????? ??? */}
        {/* ?????? ?????? ?????? */}
        <Box margin={{ top: "20px" }} gap="medium">
          <StyledText
            text="?????? ????????????????"
            size="22px"
            style={{
              width: "100%",
              textAlign: "center",
              fontFamily: "gwtt",
            }}
          />
          <Box direction="row">
            {array.map((el) => (
              <img
                key={el}
                onClick={() => handleStarClick(el)}
                src={clicked[el] ? Star : StarBlank}
              />
            ))}
          </Box>
        </Box>
        {/* ?????? ?????? ??? */}
        {/* ?????? ?????? ?????? ?????? */}

        {/* ?????? ?????? ?????? ??? */}
        {/* ?????????????????? ?????? */}
        <Box margin="small" width="90%" align="center">
          <Box width="90%" align="end">
            <label
              htmlFor="image"
              style={{
                display: "flex",
                alignContent: "end",
              }}
            >
              <ImageBtn src={ImInput} />
              <StyledText
                size="13px"
                color={image ? "black" : "lightgray"}
                text={image ? "?????? ??????" : "?????? 1???"}
                alignSelf="end"
              />
            </label>
            <input
              id="image"
              type="file"
              accept="image/jpg,image/png,image/jpeg,image/gif"
              style={{
                display: "none",
              }}
              onChange={handleImageUpload}
            />
          </Box>
          <STextArea
            placeholder="????????? ?????? ????????? ??????????????????!"
            onChange={(e) => handleText(e.target.value)}
            value={reviewDesc}
          />
          <Box justify="end" direction="row" width="90%">
            <div>{reviewDesc.length} / 50</div>
          </Box>
        </Box>
        {/* ????????? ????????? ??? */}
        {/* ?????? ?????? */}
        <Box margin={{ top: "20px", left: "15px", bottom: "10px" }}>
          <StyledText text="Tag (?????? 5???)" size="14px" weight="bold" />
          <FlexBox Column_C>
            <FlexBox
              Row_S
              style={{
                flexWrap: "wrap",
                padding: "6px 10px",
                width: "88vw",
              }}
            >
              {tags.map((tag, idx) =>
                select.some((v) => v.id === tag.id) ? (
                  <Button
                    key={idx}
                    onClick={() => {
                      setSelect(
                        select.filter((Button) => Button.id !== tag.id)
                      );
                    }}
                    taggreen="true"
                    style={{
                      margin: "6px 3px ",
                      wordWrap: "break-word",
                      minWidth: "22%",
                      padding: "5px 3px",
                    }}
                  >
                    #{tag.searchTagName}
                  </Button>
                ) : (
                  <Button
                    key={idx}
                    onClick={() => {
                      select.length === 5
                        ? setVisible(true)
                        : setSelect((select) => [...select, tag]);
                    }}
                    taggray="true"
                    style={{
                      margin: "6px 3px ",
                      wordWrap: "break-word",
                      minWidth: "22%",
                      padding: "5px 3px",
                    }}
                  >
                    #{tag.searchTagName}
                  </Button>
                )
              )}
            </FlexBox>
          </FlexBox>
        </Box>
        {/* ?????? ??? */}
        <StyledText
          text="??????, ??????, ????????? ???????????????!"
          style={{
            fontFamily: "gwtt",
          }}
        />
        {/* <Box direction="row" gap="small">
          <StyledText
            text={`?????? : ${clicked.filter(Boolean).length > 0 ? "O" : "X"}`}
          />
          <StyledText text={`?????? : ${image !== undefined ? "O" : "X"}`} />
          <StyledText text={`?????? : ${select.length !== 0 ? "O" : "X"}`} />
        </Box> */}
        {/* ?????? ?????? ?????? */}
        <RegisterButton
          onClick={() => {
            if (isValied()) setOpen(true);
            else setNotValid(true);
          }}
          whileTap={{ scale: 1.2 }}
        >
          ??????
        </RegisterButton>
        <AlertDialog
          open={open}
          handleClose={() => {
            setOpen(false);
          }}
          handleAction={handleRegister}
          title="?????? ??????"
          desc="????????? ?????????????????????????"
          cancel="??????"
          accept="??????"
          register
        />

        <AlertDialog
          open={notValid}
          handleClose={() => {
            setNotValid(false);
          }}
          title="?????? ??????"
          desc="?????? ????????? ???????????????!"
          cancel="??????"
          accept={undefined}
        />
        <AlertDialog
          open={exit}
          handleClose={() => {
            setExit(false);
          }}
          handleAction={() => {
            navigate("/");
          }}
          title="?????? ??????"
          desc="?????? ????????? ?????????????????????????"
          accept="?????????"
          cancel="??????"
        />
        {visible && (
          <Grommet theme={GrommetTheme}>
            <Notification
              toast={{ position: "center" }}
              title="????????? 5?????????"
              status={"normal"}
              onClose={() => setVisible(false)}
            />
          </Grommet>
        )}
      </Box>
    </Box>
  );
};
