package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/directors")
public class DirectorController {

	private final DirectorService directorService;

	@Autowired
	public DirectorController(DirectorService directorService) {
		this.directorService = directorService;
	}

	@GetMapping
	public List<Director> getAllDirectors() {
		return directorService.getAllDirectors();
	}

	@GetMapping("/{id}")
	public Director getDirectorById(@PathVariable int id) {
		return directorService.getDirectorById(id);
	}

	@PostMapping
	public Director postDirector(@Valid @RequestBody Director director) {
		return directorService.addDirector(director);
	}

	@PutMapping
	public Director putDirector(@Valid @RequestBody Director director) {
		return directorService.updateDirector(director);
	}

	@DeleteMapping("/{id}")
	public void deleteDirector(@PathVariable int id) {
		directorService.deleteDirector(id);
	}
}
