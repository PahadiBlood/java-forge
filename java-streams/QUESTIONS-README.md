# Java Streams Interview Questions (Based on Project)

## Core Stream Concepts
1. What is a Java Stream and how is it different from a Collection?
2. What are the key characteristics of streams?
3. What is lazy evaluation in streams?
4. What is the difference between intermediate and terminal operations?
5. Can a stream be reused after a terminal operation?
6. How does stream pipeline processing work internally?
7. What is the role of Spliterator in streams?
8. What is the difference between stream() and parallelStream()?
9. How does short-circuiting work in streams?
10. What are stateless vs stateful operations?

## Filtering & Mapping
11. What is the difference between map() and flatMap()?
12. When would you use flatMap instead of map?
13. How does filter() work internally?
14. Can filter() modify elements?
15. What happens if map() returns null?
16. How would you transform a list of objects into another list using streams?
17. How to flatten a nested collection using flatMap?

## Collect & Collectors
18. What is the collect() method in streams?
19. Explain the three arguments of collect():
    - supplier
    - accumulator
    - combiner
20. What is the difference between collect() and reduce()?
21. What is Collector in Java?
22. What are some commonly used collectors?
23. How does Collectors.toList() work internally?
24. What is Collectors.toMap() and what problems can occur?
25. How do you handle duplicate keys in toMap()?
26. What is Collectors.groupingBy()?
27. Difference between groupingBy() and partitioningBy()?
28. What is downstream collector?
29. How to create a custom collector?

## Reduce Operation
30. What is the reduce() method?
31. Explain reduce() with identity, accumulator, and combiner.
32. What happens if identity is not provided?
33. Difference between reduce() and collect()?
34. When should reduce() be avoided?
35. How does reduce behave in parallel streams?

## Sorting & Distinct
36. How does distinct() work internally?
37. What is required for distinct() to work correctly?
38. How does sorted() work?
39. Difference between sorted() and sort() on collections?
40. Can you use custom comparator in streams?

## Finding & Matching
41. What is findFirst() vs findAny()?
42. How does findAny() behave in parallel streams?
43. What are anyMatch(), allMatch(), noneMatch()?
44. Are these operations short-circuiting?
45. What happens if stream is empty in findFirst()?

## Numeric Streams
46. What is IntStream, LongStream, DoubleStream?
47. Why do we need primitive streams?
48. Difference between map() and mapToInt()?
49. What is summaryStatistics()?
50. How to calculate average using streams?

## Grouping & Aggregation
51. How to group objects by a field using streams?
52. How to count elements in each group?
53. How to find max/min per group?
54. How to group by multiple fields?
55. How to convert grouping result into a custom structure?

## FlatMap Deep Dive
56. Explain flatMap with an example of nested lists.
57. How flatMap works with Optional?
58. Difference between flatMap and map in Optional?

## Parallel Streams
59. What is a parallel stream?
60. How does parallel stream improve performance?
61. When should you NOT use parallel streams?
62. What is ForkJoinPool?
63. What problems can occur with parallel streams?
64. Are streams thread-safe?
65. What is the role of combiner in parallel streams?

## Performance & Best Practices
66. Why streams can be slower than loops sometimes?
67. How to optimize stream performance?
68. When to prefer loops over streams?
69. What are common pitfalls of streams?
70. What is boxing and unboxing issue in streams?

## Exception Handling
71. How to handle exceptions in streams?
72. Can checked exceptions be used inside streams?
73. What are workarounds for checked exceptions?

## Real-world Scenarios
74. How to remove duplicates from a list using streams?
75. How to find second highest number using streams?
76. How to count frequency of elements?
77. How to merge two lists using streams?
78. How to convert list to map?
79. How to sort map by values using streams?
80. How to filter and transform in one pipeline?

## Coding Style / Design
81. Difference between imperative and declarative approach?
82. Why streams are considered functional programming?
83. What is immutability in streams?
84. Can streams modify original data?
85. What is method reference vs lambda?

## Advanced
86. What is peek() used for?
87. Why peek() should be used carefully?
88. What is stream builder?
89. How to create infinite streams?
90. What is limit() and skip()?
91. How does takeWhile() and dropWhile() work?
92. What is unordered() in streams?
93. What is stream pipeline fusion?

## Based on Your Project (Specific Style Questions)
94. In CollectTest, why use custom collector instead of toList()?
95. In GroupingTest, how would you group Person by age and city?
96. In ReduceTest, how would you calculate total salary?
97. In FlatMapTest, how would you flatten list of cars per person?
98. In MinMaxTest, how would you find youngest person?
99. In JoiningStringTest, how does Collectors.joining() work?
100. In DistinctTest, what ensures uniqueness for Person object?
101. In FilterAndMapAndAverageTest, how would you calculate average age?
102. In IntStreamTest, why prefer IntStream over Stream<Integer>?
103. In FindAnyTest, when is findAny better than findFirst?

# Additional Java Stream Practice Questions (Practical)

## Filtering (5 Questions)
104. Given a list of integers, filter out all even numbers and return only odd numbers.
105. From a list of strings, filter out values that start with a vowel.
106. Given a list of employees, filter those whose salary is greater than 50,000.
107. From a list of transactions, filter only successful transactions.
108. Given a list of dates, filter dates that fall within the current year.

## Mapping (5 Questions)
109. Convert a list of strings to uppercase using streams.
110. Given a list of numbers, return their squares.
111. From a list of users, extract only their email addresses.
112. Convert a list of prices from INR to USD.
113. Given a list of sentences, map them to their word counts.

## FlatMap (5 Questions)
114. Flatten a list of lists of integers into a single list.
115. Given a list of sentences, split them into words and flatten into one list.
116. From a list of orders, each containing multiple items, get all items in a single list.
117. Flatten a list of Optional values into a stream of present values.
118. Given nested arrays, convert them into a single stream.

## Collect (5 Questions)
119. Collect a stream of integers into a Set.
120. Convert a list of strings into a Map where key is string and value is its length.
121. Group a list of employees by department.
122. Partition numbers into even and odd using collectors.
123. Collect names into a single comma-separated string.

## Reduce (5 Questions)
124. Find the sum of all numbers in a list using reduce.
125. Find the product of all elements in a list.
126. Find the maximum value using reduce.
127. Concatenate a list of strings using reduce.
128. Count total characters across all strings using reduce.

## Sorting & Distinct (5 Questions)
129. Sort a list of integers in ascending order.
130. Sort a list of strings in descending order.
131. Remove duplicate elements from a list.
132. Sort a list of objects based on a specific field.
133. Sort a map by its values using streams.

## Finding & Matching (5 Questions)
134. Find the first element greater than 100.
135. Check if all numbers are positive.
136. Check if any string contains a specific keyword.
137. Verify that no element is null in a list.
138. Find any random element from a stream.

## Numeric Streams (5 Questions)
139. Calculate the average of a list of integers.
140. Find the sum of squares of numbers.
141. Generate a range of numbers from 1 to 100.
142. Find the maximum number using IntStream.
143. Convert Stream<Integer> to IntStream.

## Grouping & Aggregation (5 Questions)
144. Group numbers by even and odd.
145. Count frequency of each word in a list.
146. Group employees by salary range.
147. Find average salary per department.
148. Find highest value in each group.

## Parallel Streams (5 Questions)
149. Convert a list into a parallel stream and process it.
150. Compare performance of stream vs parallelStream.
151. Process a large dataset using parallel streams.
152. Demonstrate thread usage in parallel streams.
153. Aggregate results using parallel streams safely.