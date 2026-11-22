package com.niskora.identity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface UserRepository extends JpaRepository<User, UUID> {

  /** The user with this login name whatever its case, as the unique index reads it. */
  @Query("SELECT u FROM User u WHERE lower(u.loginName) = lower(:loginName)")
  Optional<User> findByLoginName(String loginName);

  /**
   * Holds back every other user being created until this transaction ends, so a check of the users
   * that exist stays true until then. Reading the users goes on meanwhile.
   */
  @Modifying
  @Query(value = "LOCK TABLE identity.users IN SHARE ROW EXCLUSIVE MODE", nativeQuery = true)
  void lockAgainstNewUsers();

  /** Whether the user with this id exists and may use the app: their account is not disabled. */
  boolean existsByIdAndDisabledFalse(UUID id);

  /**
   * The user with this id, with their row locked until this transaction ends, so the version a
   * change checks stays the current one until the change is saved (ENG-87).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findForChange(UUID id);
}
