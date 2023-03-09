package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.film.FilmNotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.film.dao.FilmDao;
import ru.yandex.practicum.filmorate.storage.film.dao.GenreDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component("filmDbStorage")
@Primary
@Slf4j
public class FilmDbDao implements FilmDao {
    private final String GET_POPULAR_FILMS = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id, '-', g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "SELECT fi.* " +
            "        FROM films fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        ORDER BY clike DESC LIMIT(?)" +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "GROUP BY f.film_id";

    private final String GET_POPULAR_FILMS_YEAR = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id, '-', g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM (" +
            "SELECT fk.*, fg.genre_id FROM films fk " +
            "LEFT JOIN films_genre fg " +
            "ON fk.film_id = fg.film_id " +
            ") fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id " +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        WHERE YEAR(fi.release_date) = ? " +
            "        ORDER BY clike DESC LIMIT(?)" +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "GROUP BY f.film_id";

    private final String GET_POPULAR_FILMS_GENRE = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id, '-', g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM (" +
            "SELECT fk.*, fg.genre_id FROM films fk " +
            "LEFT JOIN films_genre fg " +
            "ON fk.film_id = fg.film_id " +
            ") fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        WHERE fi.genre_id = ? " +
            "        ORDER BY clike DESC LIMIT(?)" +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "GROUP BY f.film_id";

    private final String GET_POPULAR_FILMS_YEAR_GENRE = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id, '-', g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM (" +
            "SELECT fk.*, fg.genre_id FROM films fk " +
            "LEFT JOIN films_genre fg " +
            "ON fk.film_id = fg.film_id " +
            ") fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        WHERE YEAR(fi.release_date) = ? AND fi.genre_id = ? " +
            "        ORDER BY clike DESC LIMIT(?) " +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "GROUP BY f.film_id";

    private final String GET_FILM_ID = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM FILMS f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE f.film_id = ? " +
            "GROUP BY f.film_id";

    private final String GET_FILMS = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM films f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "GROUP BY f.film_id";

    private final String GET_FILMS_ID = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM films f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE f.film_id IN (%s)" +
            "GROUP BY f.film_id";

    private final String GET_COMMON_FILMS = "SELECT u.film_id FROM" +
            "(SELECT * FROM FILMS_LIKE fl WHERE USER_ID = ?) u" +
            "INNER JOIN" +
            "(SELECT * FROM FILMS_LIKE fl WHERE USER_ID = ?) f" +
            "ON u.film_id = f.film_id" +
            "LEFT JOIN FILMS_LIKE fl ON u.film_id = fl.film_id" +
            "GROUP BY fl.film_id ORDER BY COUNT(fl.film_id) DESC)";

    private final String GET_DIRECTORS_FILMS_LIKE = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM FILMS fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        ORDER BY clike " +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE f.film_id IN ( " +
                "SELECT f2.film_id " +
                "FROM films_director f2 " +
                "WHERE f2.director_id = ?) " +
            "GROUP BY f.film_id";

    private final String GET_DIRECTORS_FILMS_YEAR = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM films f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE f.film_id IN ( " +
                "SELECT f2.film_id " +
                "FROM films_director f2 " +
                "WHERE f2.director_id = ? ) " +
            "GROUP BY f.film_id " +
            "ORDER BY f.release_date";

    private final String GET_SEARCH_FILMS_BY_NAME = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM films fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        ORDER BY clike DESC" +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE LOWER(f.name) LIKE ? " +
            "GROUP BY f.film_id " +
            "ORDER BY f.film_id DESC";

    private final String GET_SEARCH_FILMS_BY_DIRECTOR = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM films fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        ORDER BY clike DESC" +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE LOWER(d.name) LIKE ? " +
            "GROUP BY f.film_id " +
            "ORDER BY f.film_id DESC";

    private final String GET_SEARCH_FILMS_BY_ALL = "SELECT f.*, rm.*, " +
            "GROUP_CONCAT(DISTINCT Concat(g.genre_id, '-', g.genre_name) ORDER BY Concat(g.genre_id,'-',g.genre_name)) AS genre_id_name, " +
            "GROUP_CONCAT(DISTINCT Concat(d.director_id, '-', d.name) ORDER BY Concat(d.director_id, '-', d.name)) AS director_id_name " +
            "FROM (" +
            "  SELECT fi.* " +
            "        FROM films fi " +
            "        LEFT JOIN " +
            "        (SELECT film_id, COUNT(*) clike " +
            "            FROM films_like " +
            "            GROUP BY film_id" +
            "        ) fil " +
            "        ON fil.film_id = fi.film_id " +
            "        ORDER BY clike DESC" +
            ") f " +
            "LEFT JOIN ratings_mpa rm ON f.rating_id = rm.rating_id " +
            "LEFT JOIN films_genre fg ON f.film_id = fg.film_id " +
            "LEFT JOIN genre g ON fg.genre_id = g.genre_id " +
            "LEFT JOIN films_director fd ON f.film_id = fd.film_id " +
            "LEFT JOIN directors d ON fd.director_id = d.director_id " +
            "WHERE LOWER(f.name) LIKE ? " +
            "OR LOWER(d.name) LIKE ? " +
            "GROUP BY f.film_id " +
            "ORDER BY f.film_id DESC";

    private final JdbcTemplate jdbcTemplate;
    private final GenreDao genreDao;

    public FilmDbDao(JdbcTemplate jdbcTemplate,
                     @Qualifier("genreDbDao") GenreDao genreDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreDao = genreDao;
    }

    @Override
    public Film addFilm(Film film) {
        log.info("Запрос на добавление фильма: {} получен хранилищем БД", film.getName());

        //добавить информацию о фильме в таблицу films
        String addFilmSql = "INSERT INTO films (name, description, release_date, duration, rate, rating_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(addFilmSql, new String[]{"film_id"});
                    ps.setString(1, film.getName());
                    ps.setString(2, film.getDescription());
                    ps.setString(3, film.getReleaseDate().toString());
                    ps.setInt(4, film.getDuration());
                    ps.setInt(5, film.getRate());
                    ps.setInt(6, film.getMpa().getId());
                    return ps;
                },
                keyHolder);
        long filmId = keyHolder.getKey().intValue();
        film.setId(filmId);
        log.debug("Добавлен новый фильм с id={}", filmId);

        //если все жанры найдены в БД, то добавляем записи о жанрах в таблицу films_genre
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            log.debug("Добавляем жанры вновь создаваемому фильму с id={}.", filmId);
            Set<Integer> genres = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            for (int gr : genres) {
                genreDao.addFilmGenre(film.getId(), gr);
            }
            log.debug("Жанры для фильма с id={} добавлены.", filmId);
        }

        if (film.getDirectors() != null && !film.getDirectors().isEmpty())
            addDirectors(film.getDirectors(), film.getId());

        return getFilm(film.getId());
    }

    @Override
    //обновляем поля таблицы films: name, releaseDate, description, duration, rate, rating_id
    //поле rating_id сначала ищем в таблице ratings_mpa и если найден, то обновляем
    //обновляем поля таблицы films_genre:film_id, genre_id - удаляем и перезаписываем
    public Film updateFilm(Film film) {
        log.info("Получен запрос на обновление фильма с id={} в БД", film.getId());

        //обновляем данные в таблице films
        log.debug("Формируем sql запрос...");
        String updateFilmSql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, rate = ?," +
                "rating_id = ? WHERE film_id = ?";
        if (jdbcTemplate.update(updateFilmSql, film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), film.getRate(), film.getMpa().getId(), film.getId()) <= 0) {
            log.debug("Фильм с id={} для обновления не найден.", film.getId());
            throw new FilmNotFoundException("Фильм с id=" + film.getId() + " для обновления не найден.");
        }
        log.debug("Фильм с id={} обновлён.", film.getId());
        //если все жанры найдены в БД, то сначала удаляем записи из films_genre
        // потом добавляем записи о жанрах в таблицу films_genre
        genreDao.delFilmGenre(film.getId());
        log.debug("Обновляем жанры фильма с film_id={}, жанры: {}", film.getId(), film.getGenres());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> genres = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            log.debug("id жанров для обновления: {}", genres);
            for (int gr : genres) {
                genreDao.addFilmGenre(film.getId(), gr);
            }
            log.debug("Жанры фильма с film_id={} обновлены.", film.getId());
        }

        updateDirectors(film.getDirectors(), film.getId());

        return getFilm(film.getId());
    }

    @Override
    public void deleteFilm(long filmId) {
        log.debug("Получен запрос на удаление фильма с id={}", filmId);
        String deleteFilmSql = "DELETE FROM films WHERE film_id = ?";
        if (jdbcTemplate.update(deleteFilmSql, filmId) <= 0) {
            log.debug("Фильм с id={} для удаления не найден.", filmId);
            throw new FilmNotFoundException("Фильм с id=" + filmId + " для удаления не найден.");
        }
        log.debug("Фильм с id={} удалён.", filmId);
    }

    @Override
    //возвращаемые поля:
    //из таблицы films: film_id, name, description, release_date, duration, rate,
    //genre - Set: genre.id...
    //из таблицы ratings_mpa: mpa.id,mpa.name
    public Film getFilm(long filmId) {
        log.debug("Получен запрос на фильм с id={};", filmId);
        List<Film> films = jdbcTemplate.query(GET_FILM_ID, (rs, rowNum) -> filmMapper(rs), filmId);

        if (films.isEmpty()) {
            log.debug("Фильм не найден.");
            throw new FilmNotFoundException("Фильм не найден.");
        }
        return films.iterator().next();
    }

    @Override
    public List<Film> getFilms() {
        log.debug("Получен запрос на чтение всех фильмов");
        //запрашиваем все фильмы с жанрами и рейтингом MPA
        List<Film> films = jdbcTemplate.query(GET_FILMS, (rs, rowNum) -> filmMapper(rs));
        log.debug("Получен список из {} фильмов.", films.size());
        return films;
    }

    @Override
    public List<Film> getFilms(List<Long> filmsId) {
        log.debug("FilmDbDao: Получен запрос на чтение нескольких фильмов с определённым filmId");
        log.debug("FilmDbDao: получены filmsId рекомендуемых фильмов: {}", filmsId.toString());
        String inSql = String.join(",", Collections.nCopies(filmsId.size(), "?"));
        String getFilmSql = String.format(GET_FILMS_ID, inSql);

        //запрашиваем все фильмы с жанрами и рейтингом MPA
        List<Film> films = jdbcTemplate.query(getFilmSql, (rs, rowNum) -> filmMapper(rs), filmsId.toArray());
        log.debug("Получен список из {} фильмов.", films.size());
        return films;
    }

    @Override
    public List<Film> getPopularFilmGenreIdYear(long count, long genreId, long year) {
        if (year == 0 && genreId == 0) {
            log.debug("Extracting {} popular films from the database", count);
            return jdbcTemplate.query(GET_POPULAR_FILMS, (rs, rowNum) -> filmMapper(rs), count);
        } else if (year > 0 && genreId == 0) {
            log.debug("Фильтры запроса: count={}, year={}",count,year);
            return jdbcTemplate.query(GET_POPULAR_FILMS_YEAR, (rs, rowNum) -> filmMapper(rs), year, count);
        } else if (year == 0 && genreId > 0) {
            log.debug("Фильтры запроса: count={}, genreId={}",count,year);
            return jdbcTemplate.query(GET_POPULAR_FILMS_GENRE, (rs, rowNum) -> filmMapper(rs), genreId, count);
        } else {
            log.debug("Фильтры запроса: count={}, year={}, genreId={}", count, year, genreId);
            return jdbcTemplate.query(GET_POPULAR_FILMS_YEAR_GENRE, (rs, rowNum) -> filmMapper(rs),
                    year, genreId, count);
        }
    }

    @Override
    public List<Film> getDirectorsFilms(int directorId, String sortBy) {
        log.debug("Request to get directors films from DB, sortBy={}", sortBy);

        String sql = "";

        if (sortBy.equals("likes")) {
            log.debug("Начат вывод фильмов с сортировкой Likes");
            sql = GET_DIRECTORS_FILMS_LIKE;
        } else {
            log.debug("Начат вывод фильмов с сортировкой Years");
            sql = GET_DIRECTORS_FILMS_YEAR;
        }
        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> filmMapper(rs), directorId);
        log.debug("Получен список из {} фильмов.", films.size());
        return films;
    }

    @Override
    public List<Film> getCommonFilms(long userId, long friendId) {
        log.info("FilmDb: Запрос на получение общих фильмов пользователей с userId={} и friendId={}...", userId, friendId);
        return getFilms(jdbcTemplate.query(GET_COMMON_FILMS, (rs, rowNum) -> rs.getLong("film_id"),
                userId, friendId));
    }

    private Film filmMapper(ResultSet rs) throws SQLException {
        //перебираем записи результирующего набора
        return Film.builder()
                .id(rs.getLong("film_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .rate(rs.getInt("rate"))
                .mpa(MPA.builder()
                        .id(rs.getInt("rating_id"))
                        .name((rs.getString("rating_name")))
                        .build())
                .genres(getGenresFromResultSet(rs))
                .directors(getDirectorsFromResultSet(rs))
                .build();
    }

    private Set<Director> getDirectorsFromResultSet(ResultSet rs) throws SQLException {
        Set<Director> directors = new HashSet<>();

        String allFilmDirectors = rs.getString("director_id_name");

        if (allFilmDirectors == null || allFilmDirectors.isEmpty() || allFilmDirectors.isBlank() || allFilmDirectors.equals("-"))
            return directors;

        for (String filmDirector : allFilmDirectors.split(",")) {
            String[] strings = filmDirector.split("-");
            directors.add(
                    Director.builder()
                            .id(Integer.parseInt(strings[0]))
                            .name(strings[1])
                            .build()
            );
        }

        return directors;
    }

    private Set<Genre> getGenresFromResultSet(ResultSet rs) throws SQLException {
        Set<Genre> genres = new HashSet<>();

        String allFilmGenres = rs.getString("genre_id_name");

        if (allFilmGenres == null || allFilmGenres.isEmpty() || allFilmGenres.isBlank() || allFilmGenres.equals("-"))
            return genres;

        for (String filmGenre : allFilmGenres.split(",")) {
            String[] strings = filmGenre.split("-");
            genres.add(
                    Genre.builder()
                            .id(Integer.parseInt(strings[0]))
                            .name(strings[1])
                            .build()
            );
        }

        return genres;
    }

    private void addDirectors(Set<Director> directors, long filmId) {
        log.debug("Request to add directors to DB.");
        String sql = "INSERT INTO films_director (film_id, director_id) VALUES (?, ?)";
        for (Director director : directors) {
            jdbcTemplate.update(sql, filmId, director.getId());
        }
    }

    private void updateDirectors(Set<Director> directors, long filmId) {
        log.debug("Request to update directors to film with id = {}", filmId);
        String sql = "DELETE FROM films_director WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
        if (directors != null)
            addDirectors(directors, filmId);
    }

    @Override
    public List<Film> searchFilms(Optional<String> query, List<String> by) {
        List<Film> searchedFilms = new ArrayList<>();
        if (query.get().isEmpty() || query.get().equals(" ")) {
            return searchedFilms;
        }
        String stringInSql = "%" + query.get().toLowerCase() + "%";
        if (by != null) {
            log.debug("Получен запрос с параметром by");
            if (by.size() == 1 & by.contains("title")) {
                log.debug("Получен запрос на поиск фильма по названию");
                return getSearchedFilms(GET_SEARCH_FILMS_BY_NAME, stringInSql);
            }
            if (by.size() == 1 & by.contains("director")) {
                log.debug("Получен запрос на поиск фильма по имени режиссера");
                return getSearchedFilms(GET_SEARCH_FILMS_BY_DIRECTOR, stringInSql);
            }
            if (by.size() == 2 & by.contains("title") & by.contains("director")) {
                log.debug("Получен запрос на поиск фильма по имени режиссера и по названию фильма");
                searchedFilms = jdbcTemplate.query(GET_SEARCH_FILMS_BY_ALL, (rs, rowNum) -> filmMapper(rs), stringInSql, stringInSql);
                log.debug("Результаты поиска:");
                for (Film film : searchedFilms) {
                    log.debug("Фильм с film_id={}: {}", film.getId(), film);
                }
                return searchedFilms;
            } else {
                throw new IllegalArgumentException("Передан некорректный параметр by!");
            }
        }
        log.debug("Получен запрос без параметра by, выполнен поиск по умолчанию");
        return getSearchedFilms(GET_SEARCH_FILMS_BY_NAME, stringInSql);
    }

    private List<Film> getSearchedFilms(String sql, String stringInSql) {
        List<Film> searchedFilms = jdbcTemplate.query(sql, (rs, rowNum) -> filmMapper(rs), stringInSql);
        log.debug("Результаты поиска:");
        for (Film film : searchedFilms) {
            log.debug("Фильм с film_id={}: {}", film.getId(), film);
        }
        return searchedFilms;
    }

}
