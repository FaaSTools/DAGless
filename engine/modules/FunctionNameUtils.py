import re


def get_memory_from_function_name(function_name: str) -> int:
    """Returns the memory from the function name"""
    regex = r"(\d{3,5}[a-zA-Z]{2})$"
    return int(re.search(regex, function_name).group()[:-2])


def get_memory_unit_from_function_name(function_name: str) -> str:
    """Returns the memory unit from the function name"""
    regex = r"(\d{3,5}[a-zA-Z]{2})$"
    unit = re.search(regex, function_name).group()[-2:]
    if unit.upper() == "MB":
        return "MB"
    if unit.upper() == "MI":
        return "MI"
    else:
        raise Exception("Unknown memory unit")


def get_order_from_function_name(function_name: str) -> int:
    """Returns the order from the function name"""
    regex = r"\d+-"
    return int(re.search(regex, function_name).group()[:-1])


def get_region_from_function_name(function_name: str, provider: str) -> str:
    """Returns the region from the function name"""
    if provider == "aws":
        regex = r"[a-z]{2}-[a-z]+-\d{1}"
        return re.search(regex, function_name).group()
    elif provider == "gcp":
        regex = r"[a-z]+-[a-z]+\d{1}"
        return re.search(regex, function_name).group()
    else:
        raise Exception("Unknown provider")


def get_provider_from_function_name(function_name):
    """Returns the provider from the function name"""
    return function_name[:3]
