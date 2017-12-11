package com.andrewbarraford.s3backup.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;


public interface EventRepository extends JpaRepository<Event, Integer> {

    Collection<Event> findAllByBackedUp(boolean isBackedUp);

    Collection<Event> findAllByBackUpCompleteAndStartTimeBefore(boolean backedUp, Date startTime);

    Optional<Event> findOneById(final int id);

}
