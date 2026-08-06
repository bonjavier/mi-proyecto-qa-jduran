package co.com.simon.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UniqueRandomNumber {

    private static List<Integer> availableNumbers = new ArrayList<>();

    static {
        resetNumbers();
    }

    public static void resetNumbers() {
        availableNumbers.clear();
        for (int i = 1; i <= 6; i++) {
            availableNumbers.add(i);
        }
        Collections.shuffle(availableNumbers);
    }

    public static int getNextNumber() {
        if (availableNumbers.isEmpty()) {
            throw new RuntimeException("No quedan más items disponibles para seleccionar (se agotaron los números del 1 al 6).");
        }
        return availableNumbers.remove(0);
    }
}
