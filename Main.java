import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // Сканер для введення даних користувачем
        System.out.println("Введіть шлях до директорії:"); // Запит на введення шляху
        String directoryPath = scanner.nextLine();

        File directory = new File(directoryPath); // Перевірка директорії
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Некоректна директорія. Завершення програми."); // Помилка, якщо шлях некоректний
            return;
        }

        ExecutorService executor = Executors.newCachedThreadPool(); // Використання ThreadPool для задач
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool(); // Використання ForkJoin для асинхронного виконання

        try {
            // Виконання пошуку зображень у вибраній директорії
            List<File> imageFiles = forkJoinPool.invoke(new ImageSearchTask(directory));

            // Виведення кількості знайдених файлів
            System.out.println("Кількість знайдених зображень: " + imageFiles.size());

            // Якщо знайдено хоча б одне зображення, відкриваємо останнє
            if (!imageFiles.isEmpty()) {
                File lastImage = imageFiles.get(imageFiles.size() - 1);
                System.out.println("Відкриваю останнє зображення: " + lastImage.getAbsolutePath());
                openFile(lastImage); // Виклик методу для відкриття файлу
            }
        } finally {
            executor.shutdown(); // Завершення роботи ThreadPool
        }
    }

    /**
     * Відкриває файл у стандартному переглядачі ОС
     *
     * @param file Файл для відкриття
     */
    private static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file); // Відкриття файлу через системний переглядач
        } catch (IOException e) {
            System.out.println("Не вдалося відкрити файл: " + file.getAbsolutePath()); // Повідомлення про помилку
        }
    }

    /**
     * Клас для рекурсивного пошуку зображень у директорії
     */
    static class ImageSearchTask extends RecursiveTask<List<File>> {
        private final File directory;

        public ImageSearchTask(File directory) {
            this.directory = directory; // Директорія для пошуку
        }

        @Override
        protected List<File> compute() {
            // Рекурсивний пошук у піддиректоріях
            List<ImageSearchTask> tasks = Stream.of(directory.listFiles())
                    .filter(File::isDirectory) // Фільтруємо тільки піддиректорії
                    .map(ImageSearchTask::new) // Створюємо нові задачі для них
                    .collect(Collectors.toList());

            for (ImageSearchTask task : tasks) {
                task.fork(); // Асинхронно запускаємо задачі
            }

            // Пошук зображень у поточній директорії
            List<File> images = Stream.of(directory.listFiles())
                    .filter(file -> !file.isDirectory() && isImage(file)) // Фільтруємо тільки файли з розширеннями зображень
                    .collect(Collectors.toList());

            // Збирання результатів із піддиректорій
            for (ImageSearchTask task : tasks) {
                images.addAll(task.join()); // Додаємо результати завершених задач
            }

            return images; // Повертаємо список знайдених зображень
        }

        /**
         * Перевіряє, чи є файл зображенням за розширенням
         *
         * @param file Файл для перевірки
         * @return true, якщо файл є зображенням
         */
        private boolean isImage(File file) {
            String fileName = file.getName().toLowerCase(); // Переводимо ім'я у нижній регістр
            // Перевірка розширень
            return fileName.endsWith(".jpg") || fileName.endsWith(".png") ||
                    fileName.endsWith(".bmp") || fileName.endsWith(".jpeg");
        }
    }
}
