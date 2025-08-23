# Advanced Python example for testing enhanced DavaJava migration
import math
from typing import List, Optional

class DataProcessor:
    def __init__(self, name: str):
        self.name = name
        self.data = []
        self.processed = False
        
    def add_item(self, item: int) -> None:
        self.data.append(item)
        
    def process_data(self) -> List[int]:
        if not self.data:
            return []
            
        result = []
        for item in self.data:
            if item > 0:
                processed_item = item * 2
                result.append(processed_item)
                
        self.processed = True
        return result
        
    def get_statistics(self) -> dict:
        if not self.processed:
            self.process_data()
            
        total = sum(self.data)
        count = len(self.data)
        average = total / count if count > 0 else 0
        
        return {
            'total': total,
            'count': count,
            'average': average,
            'max': max(self.data) if self.data else 0
        }

def create_processor(name: str, initial_data: List[int] = None) -> DataProcessor:
    processor = DataProcessor(name)
    
    if initial_data:
        for item in initial_data:
            processor.add_item(item)
            
    return processor

def main():
    data = [1, 2, 3, 4, 5]
    processor = create_processor("Main Processor", data)
    
    results = processor.process_data()
    stats = processor.get_statistics()
    
    print(f"Processed {len(results)} items")
    print(f"Statistics: {stats}")

if __name__ == "__main__":
    main()