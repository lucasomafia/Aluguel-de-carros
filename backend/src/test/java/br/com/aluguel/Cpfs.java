package br.com.aluguel;

import java.util.concurrent.ThreadLocalRandom;

final class Cpfs {
    private Cpfs() {}

    static String gerar() {
        StringBuilder cpf = new StringBuilder();
        for (int i = 0; i < 9; i++) cpf.append(ThreadLocalRandom.current().nextInt(10));
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            cpf.append((soma * 10) % 11 % 10);
        }
        return cpf.chars().distinct().count() == 1 ? gerar() : cpf.toString();
    }
}
