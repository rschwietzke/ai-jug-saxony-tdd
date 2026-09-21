package org.jugsaxony.tdd.demo2;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms2g", "-Xmx2g"})
@State(Scope.Benchmark)
public class TDDHashMapBenchmark {

    @Param({"8", "128", "1024", "8192"})
    public int size;

    private TDDHashMap<String, String> map;
    private String[] existingKeys;
    private String[] missingKeys;
    private String[] values;
    private int mask;
    private int index;

    @Setup(Level.Trial)
    public void setup() {
        mask = size - 1;
        map = new TDDHashMap<>();

        existingKeys = new String[size];
        missingKeys = new String[size];
        values = new String[size];

        Random rnd = new Random(42);
        for (int i = 0; i < size; i++) {
            existingKeys[i] = "key_" + i + "_" + rnd.nextInt(1_000_000);
            missingKeys[i] = "miss_" + i + "_" + rnd.nextInt(1_000_000);
            values[i] = "val_" + i;
            map.put(existingKeys[i], values[i]);
        }
    }

    @Benchmark
    public String getHit() {
        return map.get(existingKeys[index++ & mask]);
    }

    @Benchmark
    public String getMiss() {
        return map.get(missingKeys[index++ & mask]);
    }

    @Benchmark
    public String update() {
        int idx = index++ & mask;
        return map.put(existingKeys[idx], values[idx]);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .parent(new CommandLineOptions(args))
                .include(TDDHashMapBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
