package com.mahirung.rpgcore.data;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 데이터 저장/로드 인터페이스
 * - 현재는 PlayerData가 직접 파일 I/O를 처리하지만,
 *   향후 DB 연동, Redis 캐싱, 클라우드 저장소 등을 붙일 때 이 인터페이스를 구현하면 됨.
 */
public interface Storage {

    /**
     * 플레이어 데이터를 비동기로 로드합니다.
     * @param uuid 로드할 플레이어 UUID
     * @return PlayerData를 반환하는 CompletableFuture
     */
    CompletableFuture<PlayerData> loadPlayerDataAsync(UUID uuid);

    /**
     * 플레이어 데이터를 비동기로 저장합니다.
     * @param playerData 저장할 데이터 객체
     * @return 저장 성공 여부를 반환하는 CompletableFuture
     */
    CompletableFuture<Boolean> savePlayerDataAsync(PlayerData playerData);

    /**
     * 서버 종료 시 모든 데이터를 동기(즉시)로 저장합니다.
     */
    void saveAllOnShutdown();
}
