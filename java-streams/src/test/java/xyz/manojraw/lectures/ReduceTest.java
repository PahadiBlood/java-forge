package xyz.manojraw.lectures;


import org.junit.Test;

import java.util.Arrays;

public class ReduceTest {

    @Test
    public void reduce() throws Exception {
        Integer[] integers = {1, 2, 3, 4, 99, 100, 121, 1302, 199};

        long sum = Arrays.stream(integers).reduce(0, Integer::sum);
        System.out.println(sum);
    }

    @Test
    public void reduceString() throws Exception {
        char[] chars = {'M', 'a', 'n', 'o', 'j'};

        String res = new String(chars)
                .chars()
                .mapToObj(c -> String.valueOf((char) c))
                .reduce("", (v1, v2) -> v1 + v2);

        System.out.println(res);
    }
}

