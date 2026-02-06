resource "aws_sqs_queue" "extract_text_dlq" {
  name                      = "${var.prefix}-extract-text-dlq"
  message_retention_seconds = 1209600 # 14 days

  tags = { Name = "${var.prefix}-extract-text-dlq" }
}

resource "aws_sqs_queue" "extract_text" {
  name                       = "${var.prefix}-extract-text"
  visibility_timeout_seconds = 900    # 15 min — long enough for OCR
  message_retention_seconds  = 604800 # 7 days
  receive_wait_time_seconds  = 20     # long polling

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.extract_text_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${var.prefix}-extract-text" }
}
