package xyz.manojraw.lectures;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import xyz.manojraw.beans.Car;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.DoubleStream;

import xyz.manojraw.mockdata.MockData;
import org.junit.Test;

public class FrequentUseMethodsSummaryStatisticsTest {
    List<Car> cars;

    @Before
    public void init() throws IOException {
        cars = MockData.getCars();
    }

    @Test
    public void count() throws Exception {

        long totalCars = cars.stream().count();
        System.out.println(totalCars);

        long totalCarsUnder70k = cars.stream().filter(c -> c.getPrice() < 70000).count();
        System.out.println(totalCarsUnder70k);
    }

    @Test
    public void min() throws Exception {
        System.out.println(
                cars.stream().min(Comparator.comparingInt(Car::getYear)).orElse(null)
        );
    }

    @Test
    public void max() throws Exception {
        System.out.println(
                cars.stream().max(Comparator.comparingDouble(Car::getPrice)).orElse(null)
        );
    }


    @Test
    public void average() throws Exception {
        DoubleStream priceStream = cars.stream().mapToDouble(Car::getPrice);
        System.out.println(
                priceStream.average().orElse(0)
        );

    }

    @Test
    public void sum() throws Exception {
        double sum = cars.stream()
                .mapToDouble(Car::getPrice)
                .sum();
        BigDecimal bigDecimalSum = BigDecimal.valueOf(sum);
        System.out.println(sum);
        System.out.println(bigDecimalSum);

    }

    @Test
    public void statistics() throws Exception {
        DoubleSummaryStatistics statistics = cars.stream()
                .mapToDouble(Car::getPrice)
                .summaryStatistics();
        System.out.println(statistics);
        System.out.println(statistics.getAverage());
        System.out.println(statistics.getCount());
        System.out.println(statistics.getMax());
        System.out.println(statistics.getMin());
        System.out.println(statistics.getSum());
    }

}