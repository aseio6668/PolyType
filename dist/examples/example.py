# Example Python file for testing DavaJava migration
def calculate_sum(a: int, b: int) -> int:
    return a + b

def process_list(numbers: list[int]) -> int:
    numbers.sort()
    return sum(numbers)

class Calculator:
    def __init__(self, initial_value: int = 0):
        self.value = initial_value
    
    def add(self, amount: int) -> int:
        self.value += amount
        return self.value
    
    def get_value(self) -> int:
        return self.value