package xyz.manojraw.lectures;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class JoiningStringTest {

    @Test
    public void joiningStrings() throws Exception {
        List<String> names = ImmutableList.of("anna", "john", "marcos", "helena", "yasmin");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i == 0) {
                builder.append(names.get(i));
            } else {

                builder.append(",").append(names.get(i));
            }
        }

        System.out.println(builder);
    }

    @Test
    public void joiningStringsWithStream() throws Exception {
        List<String> names = ImmutableList.of("anna", "john", "marcos", "helena", "yasmin");
        System.out.println(
                names.stream().collect(Collectors.joining(",","[","]"))
        );
    }
}
