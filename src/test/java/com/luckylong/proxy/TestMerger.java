/**
 * Copyright 2020-2030 luckylong1998@163.com(https://gitee.com/luckylong1998)(https://github.com/luckylong1998)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luckylong.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestMerger {

    public static void main(String[] args) {
        List<Test> list1 = Arrays.asList(
                new Test("Item1", "C001", 10),
                new Test("Item2", "C002", 20)
        );

        List<Test> list2 = Arrays.asList(
                new Test("Item3", "C003", 30),
                new Test("Item4", "C001", 40) ,// 与list1中的C001合并
                new Test("Item4", "C001", 40),
                new Test("Item4", "C001", 40),
        new Test("Item4", "C001", 40)


        );

        List<Test> mergedList = mergeLists(list1, list2);

        // 打印合并后的列表
        mergedList.forEach(test -> System.out.println(test.getName() + " - " + test.getCode() + " - " + test.getTotal()));
    }

    //public static List<Test> mergeLists(List<Test> list1, List<Test> list2) {
    //    // 使用Stream合并两个列表，并使用toMap收集器来根据code合并Test对象
    //    Map<String, Test> mergedMap = Stream.concat(list1.stream(), list2.stream())
    //            .collect(Collectors.toMap(
    //                    Test::getCode, // keyMapper: 从Test对象中提取code作为Map的键
    //                    Function.identity(), // valueMapper: 直接使用Test对象作为Map的值
    //                    // mergeFunction: 处理键冲突（即code相同的情况）
    //                    BinaryOperator.applyingBiFunction(
    //                            (existing, replacement) -> {
    //                                existing.setTotal(existing.getTotal() + replacement.getTotal()); // 更新total
    //                                // 这里可以选择保留existing的name或其他属性，或者根据需要进行其他逻辑处理
    //                                return existing; // 返回更新后的existing对象
    //                            }
    //                    )
    //            ));
    //
    //    // 将Map的值转换为List
    //    return new ArrayList<>(mergedMap.values());
    //}

    public static List<Test> mergeLists(List<Test> list1, List<Test> list2) {
        Map<String, Test> mergedMap = Stream.concat(list1.stream(), list2.stream())
                .collect(Collectors.toMap(
                        Test::getCode, // keyMapper
                        Function.identity(), // valueMapper
                        // mergeFunction: 处理键冲突
                        (existing, replacement) -> {
                            existing.setTotal(existing.getTotal() + replacement.getTotal());
                            // 这里可以根据需要保留existing的其他属性或进行其他逻辑处理
                            return existing; // 返回更新后的existing对象
                        }
                ));

        // 将Map的值转换为List
        return new ArrayList<>(mergedMap.values());
    }


    // Test类保持不变
    public static class Test {
        private String name;
        private String code;
        private Integer total;

        // 构造函数、getter和setter省略

        public Test(String name, String code, Integer total) {
            this.name = name;
            this.code = code;
            this.total = total;
        }

        // getter和setter方法
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }
}