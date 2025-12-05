#!/bin/bash
echo "--------------- 서버 배포 시작 (compose up) -----------------"

# docker compose 명령어는 기본적으로 현재 경로에서 compose를 찾아 그 내용을 읽어들이므로 있는 위치로 이동
cd /home/ubuntu/Gallae-Mallae-Server

# compose 의 image: 속성으로 정의된 모든 서비스의 최신 이미지를 원격 저장소(ECR)에서 한 번에 다 가져옴
docker compose -f prod.compose.yml pull

# 이미지를 빌드(생성)하는게 아니라 ECR에 이미지를 가져와 실행하는것이므로 --build 안붙임
docker compose -f prod.compose.yml up -d # 호스트 컴퓨터에 해당 이미지가 아예 존재하지 않을 때만 자동으로 pull하므로 이 명령어 전에 pull을 해야 최신버전 이미지를 실행함

echo "--------------- 서버 배포 끝 (compose up) -----------------"