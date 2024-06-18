from TerraformEngine import TerraformEngine

PROJECT_PATH = ""
TERRAFORM_PATH = "terraform"


def main():
    engine = TerraformEngine(PROJECT_PATH, TERRAFORM_PATH)
    engine.compile_sources()
    engine.write_terraform_template()
    engine.write_invocation_config("invocation_config_aws.json")


if __name__ == "__main__":
    main()
