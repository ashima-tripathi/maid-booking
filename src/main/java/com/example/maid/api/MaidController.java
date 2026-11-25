package com.example.maid.api;

import com.example.maid.api.dto.CreateMaidRequest;
import com.example.maid.api.dto.MaidResponse;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.search.MaidSearchService;
import com.example.maid.search.SearchCriteria;
import com.example.maid.service.MaidService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/maids")
public class MaidController {

    private final MaidService maidService;
    private final MaidSearchService searchService;

    public MaidController(MaidService maidService, MaidSearchService searchService) {
        this.maidService = maidService;
        this.searchService = searchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaidResponse onboard(@Valid @RequestBody CreateMaidRequest request) {
        return MaidResponse.from(maidService.onboard(request.toCommand()));
    }

    /** All parameters are optional; from/to (ISO-8601 instants) must be given together. */
    @GetMapping("/search")
    public List<MaidResponse> search(
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) ServiceType service,
            @RequestParam(required = false) Set<ServiceType> skills,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) BigDecimal maxHourlyRate,
            @RequestParam(required = false) Gender gender) {
        if ((from == null) != (to == null)) {
            throw new InvalidRequestException("Provide both 'from' and 'to', or neither");
        }
        SearchCriteria criteria = SearchCriteria.builder()
                .locality(locality)
                .serviceType(service)
                .requiredSkills(skills)
                .slot(from == null ? null : new TimeRange(from, to))
                .minRating(minRating)
                .maxHourlyRate(maxHourlyRate == null ? null : new Money(maxHourlyRate))
                .gender(gender)
                .build();
        return searchService.search(criteria).stream().map(MaidResponse::from).toList();
    }
}
