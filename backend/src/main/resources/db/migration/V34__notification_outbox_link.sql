-- V34: link notifications to the outbox event that produced them.
--
-- Idempotency for NotificationOutboxHandler under retry / parallel publisher:
-- a duplicate delivery of the same event cannot create a second notification.
-- Mirrors audit_log.outbox_event_id (V33).

ALTER TABLE notifications
    ADD COLUMN outbox_event_id UUID;

CREATE UNIQUE INDEX uq_notifications_outbox_event_id
    ON notifications(outbox_event_id)
    WHERE outbox_event_id IS NOT NULL;