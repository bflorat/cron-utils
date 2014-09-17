package com.cronutils.model;

import com.cronutils.model.field.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//Approach 1: [Discarded] brute force, iterate over all possible future dates until finding first matching cron.
//Approach 2: [...] precalculate possible values for each field and take nearest ones
//Approach 3: [...] nearestValue(FieldExpression, int timeValue): return plus -> method to retrieve value for field an sum
//Aproach 4: similar to previous one, but holding data that would contain possible values in structure and set them to date
class ExecutionTime {

    private ExecutionTime(Map<CronFieldName, CronField> fields){
    }

    public static ExecutionTime forCron(Cron cron){
        return new ExecutionTime(cron.retrieveFieldsAsMap());
    }

//    public DateTime afterDate(DateTime date){
//        date.plus()
//    }
//
//    public DateTime beforeDate(DateTime date){
//    }

    private List<Integer> fromFieldToTimeValues(FieldExpression fieldExpression, int max){
        List<Integer> values = Lists.newArrayList();
        if(fieldExpression instanceof And){
            values = fromFieldToTimeValues((And)fieldExpression, max);
        }
        if(fieldExpression instanceof Between){
            values = fromFieldToTimeValues((Between)fieldExpression, max);
        }
        if(fieldExpression instanceof On){
            values = fromFieldToTimeValues((On)fieldExpression, max);
        }
        if(fieldExpression instanceof Always){
            values = fromFieldToTimeValues((Always)fieldExpression, max);
        }
        Collections.sort(values);
        return values;
    }

    private List<Integer> fromFieldToTimeValues(And fieldExpression, int max){
        List<Integer> values = Lists.newArrayList();
        for(FieldExpression expression : fieldExpression.getExpressions()){
            values.addAll(fromFieldToTimeValues(expression, max));
        }
        return values;
    }

    private List<Integer> fromFieldToTimeValues(Between fieldExpression, int max){
        List<Integer> values = Lists.newArrayList();
        int every = fieldExpression.getEvery().getTime();
        for(int j = fieldExpression.getFrom(); j < fieldExpression.getTo() + 1; j+=every){
            values.add(j);
        }
        return values;
    }

    private List<Integer> fromFieldToTimeValues(On fieldExpression, int max){
        List<Integer> values = Lists.newArrayList();
        values.add(fieldExpression.getTime());
        return values;
    }

    private List<Integer> fromFieldToTimeValues(Always fieldExpression, int max){
        List<Integer> values = Lists.newArrayList();
        int every = fieldExpression.getEvery().getTime();
        for(int j = 1; j <= max; j+=every){
            values.add(j);
        }
        return values;
    }

    private int getMaxForCronField(CronFieldName cronFieldName){
        switch (cronFieldName){
            case YEAR:
                return DateTime.now().getYear() + 60;
            case MONTH:
                return 12;
            case DAY_OF_MONTH:
                return 31;
            case DAY_OF_WEEK:
                return 7;
            default:
                return 60;
        }
    }

    private Map<CronFieldName, List<Integer>> fieldsToExecutionTimes(Map<CronFieldName, CronField> fields){
        Map<CronFieldName, List<Integer>> executionTimes = Maps.newHashMap();
        Iterator<Map.Entry<CronFieldName, CronField>> iterator =
                fields.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<CronFieldName, CronField> entry = iterator.next();
            executionTimes.put(
                    entry.getKey(),
                    fromFieldToTimeValues(
                            entry.getValue().getExpression(),
                            getMaxForCronField(entry.getKey())
                    )
            );
        }
        return executionTimes;
    }

    private static class CronTimes{
        private List<Integer> seconds;
        private List<Integer> minutes;
    }

}