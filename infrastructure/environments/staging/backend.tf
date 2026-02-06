terraform {
  backend "s3" {
    bucket         = "epstein-terraform-state"
    key            = "staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "epstein-terraform-locks"
    encrypt        = true
  }
}
