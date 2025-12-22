package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.plan.dto.MemoRequest;
import com.practice.OAuth2.domain.plan.dto.MemoResponse;
import com.practice.OAuth2.domain.plan.entity.Memo;
import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import com.practice.OAuth2.domain.plan.repository.MemoRepository;
import com.practice.OAuth2.domain.plan.repository.ScheduleBlockRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemoService {

    private final MemoRepository memoRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "plan:lock:";

    // 메모 생성
    public void createMemo(Long blockId, MemoRequest request) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        Long planId = block.getPlan().getPlanId();
        String lockKey = LOCK_PREFIX + planId;

        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도해주세요.");

        try {

            // 기존에 3개(0, 1, 2)가 있다면 count는 3이다
            Integer nextOrder = memoRepository.countByScheduleBlock(block);

            Memo memo = Memo.builder()
                    .scheduleBlock(block)
                    .content(request.getContent())
                    .linkUrl(request.getLinkUrl())
                    .orderIndex(nextOrder)
                    .type(request.getType())
                    .build();

            memoRepository.save(memo);

            // [STOMP] MEMO_CREATED
            sendStompMessage(planId, "MEMO_CREATED", new MemoResponse(memo));

        } finally {
            unlock(lockKey);
        }
    }

    // 메모 수정
    public void updateMemo(Long memoId, MemoRequest request) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모입니다."));

        Long planId = memo.getScheduleBlock().getPlan().getPlanId();
        String lockKey = LOCK_PREFIX + planId;

        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도해주세요.");

        try {
            memo.update(request.getContent(), request.getLinkUrl(), request.getType());

            // [STOMP] MEMO_UPDATED
            sendStompMessage(planId, "MEMO_UPDATED", new MemoResponse(memo));

        } finally {
            unlock(lockKey);
        }
    }

    // 메모 삭제
    public void deleteMemo(Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모입니다."));

        Long planId = memo.getScheduleBlock().getPlan().getPlanId();
        String lockKey = LOCK_PREFIX + planId;

        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도해주세요.");

        try {
            memoRepository.delete(memo);

            // [STOMP] MEMO_DELETED (삭제된 ID와 소속 블록 ID 전송)
            sendStompMessage(planId, "MEMO_DELETED", new MemoResponse(memo));

        } finally {
            unlock(lockKey);
        }
    }

    // 메모 순서 이동
    public void reorderMemos(Long blockId, List<Long> memoIds) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        String lockKey = LOCK_PREFIX + block.getPlan().getPlanId();

        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도해주세요.");

        try {
            // 1. 해당 블록의 모든 메모 가져오기
            List<Memo> memos = memoRepository.findAllByScheduleBlockOrderByOrderIndexAsc(block);

            // 2. ID 리스트 순서대로 orderIndex 재설정
            // (ID 리스트: [3, 1, 2])

            for ( int i = 0; i < memoIds.size(); i++) {
                Long targetId = memoIds.get(i);

                final int index = i;
                // 메모 리스트에서 해당 ID 찾아서 순서 업데이트
                memos.stream()
                        .filter(m -> m.getMemoId().equals(targetId))
                        .findFirst()
                        .ifPresent(m -> m.changeOrder(index));
            }

            // 3. 변경된 리스트 전체를 다시 응답 (STOMP)
            // 프론트엔드는 이 리스트로 화면을 갈아끼움
            List<MemoResponse> responseList = memos.stream()
                    .map(MemoResponse::new)
                    .collect(Collectors.toList());

            sendStompMessage(block.getPlan().getPlanId(), "MEMO_REORDERED", responseList);

        } finally {
            unlock(lockKey);
        }
    }

    private void sendStompMessage(Long planId, String eventType, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("event", eventType);
        message.put("data", data);
        messagingTemplate.convertAndSend("/topic/plans/" + planId, message);
    }

    private boolean tryLock(String key) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "locked", 3, TimeUnit.SECONDS));
    }

    private void unlock(String key) {
        redisTemplate.delete(key);
    }
}