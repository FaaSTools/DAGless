GS_CLASS_A_OPERATION_COST = 0.005 / 1000
GS_CLASS_B_OPERATION_COST = 0.0004 / 1000

CLOUD_FUNCTION_OUTBOUND_RATE = 0.12

# Columns: Northern America	Europe	Asia	Indonesia	Oceania	Middle East	Latin America
# Rows: Northern America	Europe	Asia	Indonesia	Oceania	Middle East	Latin America

# According to https://cloud.google.com/storage/pricing
GS_FILE_TRANSFER_MATRIX = [[0.02, 0.05, 0.08, 0.10, 0.10, 0.11, 0.14],
                           [0.05, 0.02, 0.08, 0.10, 0.10, 0.11, 0.14],
                           [0.08, 0.08, 0.08, 0.10, 0.10, 0.11, 0.14],
                           [0.10, 0.10, 0.10, None, 0.08, 0.11, 0.14],
                           [0.10, 0.10, 0.10, 0.08, 0.08, 0.11, 0.14],
                           [0.11, 0.11, 0.11, 0.11, 0.11, 0.08, 0.14],
                           [0.14, 0.14, 0.14, 0.14, 0.14, 0.14, 0.14]]

# According to https://cloud.google.com/vpc/network-pricing
CLOUD_FUNCTION_FILE_TRANSFER_MATRIX = [
    [0.02, 0.05, 0.08, 0.1, 0.1, 0.11, 0.14, 0.11],
    [0.05, 0.02, 0.08, 0.10, 0.10, 0.11, 0.14, 0.11],
    [0.08, 0.08, 0.08, 0.10, 0.10, 0.11, 0.14, 0.11],
    [0.10, 0.10, 0.10, 0, 0.10, 0.11, 0.14, 0.11],
    [0.10, 0.10, 0.10, 0.10, 0.10, 0.11, 0.14, 0.11],
    [0.11, 0.11, 0.11, 0.11, 0.11, 0.11, 0.14, 0.11],
    [0.14, 0.14, 0.14, 0.14, 0.14, 0.14, 0.14, 0.14],
    [0.11, 0.11, 0.11, 0.11, 0.11, 0.11, 0.14, 0.11]
]

AWS_AUSTRALIA_REGIONS = ["ap-southeast-2", "ap-southeast-4"]

# Columns: us-east-2, us-east-1, us-west-1, us-west-2, af-south-1, ap-east-1, ap-south-2, ap-southeast-3, ap-southeast-4, ap-south-1, ap-northeast-3, ap-northeast-2, ap-southeast-1, ap-southeast-2 , ap-northeast-1, ca-central-1, ca-west-1, eu-central-1, eu-west-1, eu-west-2, eu-south-1, eu-west-3, eu-south-2, eu-north-1, eu-central-2, il-cenrtal-1, me-south-1, me-central-1, sa-east-1

AWS_REGIONS = regions = [
    "us-east-2",
    "us-east-1",
    "us-west-1",
    "us-west-2",
    "af-south-1",
    "ap-east-1",
    "ap-south-2",
    "ap-southeast-3",
    "ap-southeast-4",
    "ap-south-1",
    "ap-northeast-3",
    "ap-northeast-2",
    "ap-southeast-1",
    "ap-southeast-2",
    "ap-northeast-1",
    "ca-central-1",
    "ca-west-1",
    "eu-central-1",
    "eu-west-1",
    "eu-west-2",
    "eu-south-1",
    "eu-west-3",
    "eu-south-2",
    "eu-north-1",
    "eu-central-2",
    "il-central-1",
    "me-south-1",
    "me-central-1",
    "sa-east-1",
]

AWS_OUTBOUND_INTERNET_TRAFFIC_COST = [0.09, 0.09, 0.09, 0.09, 0.154, 0.12, 0.1093, 0.132, 0.114, 0.1093, 0.114, 0.126,
                                      0.12, 0.114, 0.114, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09,
                                      0.11, 0.117, 0.11, 0.15]

AWS_UPLOAD_OPERATION_COST = [0.005, 0.005, 0.0055, 0.005, 0.006, 0.005, 0.005, 0.005, 0.0055, 0.005, 0.0047, 0.0045,
                             0.005, 0.0055, 0.0047, 0.0055, 0.0055, 0.0054, 0.005, 0.0053, 0.0053, 0.0053, 0.0053,
                             0.005, 0.0054, 0.0055, 0.0055, 0.0055, 0.007]
AWS_DOWNLOAD_OPERATION_COST = [0.0004, 0.0004, 0.00044, 0.0004, 0.0004, 0.0004, 0.0004, 0.0004, 0.00044, 0.0004,
                               0.00037, 0.00035, 0.0004, 0.00044, 0.00037, 0.00044, 0.00044, 0.00043, 0.0004, 0.00042,
                               0.0004, 0.00042, 0.0004, 0.0004, 0.00043, 0.00044, 0.00044, 0.00044, 0.00056]

AWS_FILE_TRANSFER_MATRIX = [
    [0.0, 0.01, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # us-east-2
    [0.01, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # us-east-1
    [0.02, 0.02, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # us-west-1
    [0.02, 0.02, 0.02, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # us-west-2
    [0.147, 0.147, 0.147, 0.147, 0.0, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147,
     0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147, 0.147],  # af-south-1
    [0.09, 0.09, 0.09, 0.09, 0.09, 0.0, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09,
     0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09],  # ap-east-1
    [0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.0, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086,
     0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086],  # ap-south-2
    [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
     0.1, 0.1, 0.1, 0.1, 0.1, 0.1],  # ap-southeast-3
    [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.0, 0.1, 0.1, 0.1, 0.1, 0.08, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
     0.1, 0.1, 0.14, 0.14, 0.14, 0.1],  # ap-southeast-4
    [0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.0, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086,
     0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086, 0.086],  # ap-south-1
    [0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.0, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09,
     0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09],  # ap-northeast-3 Osaka
    [0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.0, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08,
     0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08],  # ap-northeast-2 Seoul
    [0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.0, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09,
     0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09],  # ap-southeast-1 Singapore
    [0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.08, 0.098, 0.098, 0.098, 0.098, 0.0, 0.098, 0.098, 0.098,
     0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098, 0.098],  # ap-southeast-2 Sydney
    [0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.0, 0.09, 0.09, 0.09, 0.09,
     0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09, 0.09],  # ap-northeast-1 Tokyo
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.0, 0.02, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # ca-central-1 Montreal
    [0.02, 0.02, 0.02, 0.02, 0.14, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.02, 0.0, 0.05, 0.05,
     0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.14, 0.14, 0.14, 0.05],  # ca-west-1 Calgary
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.0, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-central-1 Frankfurt
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.0,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-west-1 Ireland
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.02,
     0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-west-2 London
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.02,
     0.02, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-south-1 Milan
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.02,
     0.02, 0.02, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-west-3 Paris
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-south-2 Spain
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.0, 0.02, 0.02, 0.02, 0.02, 0.02],  # eu-north-1 Stockholm
    [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.05, 0.02, 0.02,
     0.02, 0.02, 0.02, 0.02, 0.02, 0.0, 0.02, 0.02, 0.02, 0.02],  # eu-central-2 Zurich
    [0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.15, 0.08, 0.08, 0.08, 0.08, 0.15, 0.08, 0.08, 0.08, 0.08, 0.08,
     0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.0, 0.08, 0.08, 0.08],  # il-central-1 Tel Aviv
    [0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105,
     0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.1105, 0.0, 0.1105,
     0.1105],  # me-south-1 Bahrain
    [0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085,
     0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.085, 0.0, 0.085],  # me-central-1 UAE
    [0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138,
     0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.138, 0.0]  # sa-east-1 Sao Paulo
]


def compute_file_transfer_cost(function_deployment) -> float:
    """Returns the file transfer cost of the function"""
    if function_deployment.storage_provider == "aws":
        return __compute_outbound_file_transfer_cost_s3(
            function_deployment) + __compute_outbound_file_transfer_cost_function(function_deployment)
    elif function_deployment.storage_provider == "gcp":
        return __compute_outbound_file_transfer_cost_gs(
            function_deployment) + __compute_outbound_file_transfer_cost_function(function_deployment)
    else:
        raise Exception("Unknown provider")


def __compute_outbound_file_transfer_cost_function(function_deployment) -> float:
    # Getting the size of the files to be uploaded in GB
    upload_size_gb = sum(file.get_size_mb() for file in function_deployment.upload_files) / 1024

    # Determine the function provider
    if function_deployment.function_provider.lower() == "aws":
        if function_deployment.storage_provider.lower() == "aws":
            if function_deployment.function_region.lower() == function_deployment.storage_region.lower():
                # If S3 and Lambda are in the same region, there is no outbound cost for S3
                outbound_cost_gb = 0
            else:
                # If S3 and Lambda are in different regions, there is an outbound cost for S3
                function_region_index = AWS_REGIONS.index(function_deployment.function_region)
                storage_region_index = AWS_REGIONS.index(function_deployment.storage_region)
                outbound_cost_gb = AWS_FILE_TRANSFER_MATRIX[function_region_index][storage_region_index]
        else:
            # If the function is on AWS, there is always an outbound cost for GS
            function_region_index = AWS_REGIONS.index(function_deployment.function_region)
            outbound_cost_gb = AWS_OUTBOUND_INTERNET_TRAFFIC_COST[function_region_index]

    elif function_deployment.function_provider.lower() == "gcp":
        # Google uses gibibytes instead of gigabytes
        upload_size_gb = __get_gibibyte_from_gigabyte(upload_size_gb)
        if function_deployment.storage_provider.lower() == "gcp":
            if function_deployment.function_region.lower() == function_deployment.storage_region.lower():
                # If GS and GCF are in the same region, there is no outbound cost for GS
                outbound_cost_gb = 0
            else:
                # If GS and GCF are in different regions, there is an outbound cost for GCF
                source_index = __get_gcp_region_index(function_deployment.function_region)
                destination_index = __get_gcp_region_index(function_deployment.storage_region)
                outbound_cost_gb = CLOUD_FUNCTION_FILE_TRANSFER_MATRIX[source_index][destination_index]
        else:
            # There is also a matrix for outbound cost to the internet, however since the thesis does not require federation this is not used
            # This default value is for all regions in Europe
            outbound_cost_gb = 0.12
    else:
        raise Exception("Unknown provider")
    return outbound_cost_gb * upload_size_gb


def __compute_outbound_file_transfer_cost_s3(function_deployment) -> float:
    number_download_files = len(function_deployment.download_files)
    number_upload_files = len(function_deployment.upload_files)

    download_size_gb = sum(file.get_size_mb() for file in function_deployment.download_files) / 1024

    storage_region_index = AWS_REGIONS.index(function_deployment.storage_region)
    # determine destination of file transfer
    if function_deployment.storage_provider.lower() == "aws":
        if function_deployment.function_region.lower() == function_deployment.storage_region.lower():
            # If S3 and Lambda are in the same region, there is no outbound cost for S3
            outbound_cost_per_gb = 0
        else:
            # If S3 and Lambda are in different regions, there is an outbound cost for S3
            function_region_index = AWS_REGIONS.index(function_deployment.function_region)
            outbound_cost_per_gb = AWS_FILE_TRANSFER_MATRIX[storage_region_index][function_region_index]
    else:
        outbound_cost_per_gb = AWS_OUTBOUND_INTERNET_TRAFFIC_COST[
            AWS_REGIONS.index(function_deployment.function_region)]

    # S3 COST
    outbound_cost_s3 = download_size_gb * outbound_cost_per_gb
    download_operations_cost = number_download_files * (AWS_DOWNLOAD_OPERATION_COST[storage_region_index] / 1000)
    upload_operations_cost = number_upload_files * (AWS_UPLOAD_OPERATION_COST[storage_region_index] / 1000)

    return outbound_cost_s3 + download_operations_cost + upload_operations_cost


def __compute_outbound_file_transfer_cost_gs(function_deployment) -> float:
    number_download_files = len(function_deployment.download_files)
    number_upload_files = len(function_deployment.upload_files)

    download_size_gb = sum(file.get_size_mb() for file in function_deployment.download_files) / 1024
    download_size_gb = __get_gibibyte_from_gigabyte(download_size_gb)

    # determine destination of file transfer
    if function_deployment.function_provider.lower() == "gcp":
        if function_deployment.function_region.lower() == function_deployment.storage_region.lower():
            # If GS and GCF are in the same region, there is no outbound cost for GS
            outbound_cost_per_gb = 0
        else:
            # If GS and GCF are in different regions, there is an outbound cost for GS
            storage_region_index = __get_gcp_region_index(function_deployment.storage_region)
            function_region_index = __get_gcp_region_index(function_deployment.function_region)
            outbound_cost_per_gb = GS_FILE_TRANSFER_MATRIX[storage_region_index][function_region_index]
    elif function_deployment.function_provider.lower() == "aws":
        # If the function is on AWS, there is always an outbound cost for GS (except for Australia)
        outbound_cost_per_gb = 0.12 if function_deployment.function_region not in AWS_AUSTRALIA_REGIONS else 0.19
    else:
        raise Exception("Unknown provider")

    # GS COST
    outbound_cost_gs = download_size_gb * outbound_cost_per_gb
    download_operations_cost = number_download_files * GS_CLASS_B_OPERATION_COST
    upload_operations_cost = number_upload_files * GS_CLASS_A_OPERATION_COST

    return outbound_cost_gs + download_operations_cost + upload_operations_cost


def __get_gcp_region_index(region: str) -> int:
    if "us-" in region or "northamerica-" in region:
        return 0
    elif "europe-" in region:
        return 1
    elif "asia-" in region and "asia-southeast2" not in region:
        return 2
    elif "asia-southeast2" in region:
        return 3
    elif "australia-" in region:
        return 4
    elif "me-" in region:
        return 5
    elif "southamerica-" in region:
        return 6
    elif "africa-" in region:
        return 7
    else:
        raise Exception("Unknown region")

def __get_gibibyte_from_gigabyte(gigabyte: float) -> float:
    return gigabyte * 0.931323

