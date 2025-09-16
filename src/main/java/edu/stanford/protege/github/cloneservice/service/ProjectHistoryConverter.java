package edu.stanford.protege.github.cloneservice.service;

import com.google.common.collect.ImmutableList;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.webprotege.revision.Revision;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Component;

/**
 * Service responsible for converting project history from ontology commit changes to WebProtégé
 * revisions.
 *
 * <p>This class handles the critical ordering logic where project history items (ordered newest to
 * oldest) are converted to revisions (ordered oldest to newest with sequential revision numbers).
 * The last project history item becomes the first revision.
 */
@Component
public class ProjectHistoryConverter {

  private final ChangeCommitToRevisionConverter changeCommitToRevisionConverter;

  public ProjectHistoryConverter(
      @Nonnull ChangeCommitToRevisionConverter changeCommitToRevisionConverter) {
    this.changeCommitToRevisionConverter =
        Objects.requireNonNull(
            changeCommitToRevisionConverter, "changeCommitToRevisionConverter cannot be null");
  }

  /**
   * Converts a list of OntologyCommitChange to a list of Revisions with correct ordering.
   *
   * <p>The input list is ordered from newest to oldest commits (HEAD backwards). This method
   * reverses the list so that the oldest changes become the first revisions with sequential
   * revision numbers starting from 1.
   *
   * <p><strong>Ordering Logic:</strong>
   *
   * <ul>
   *   <li>Input: {@code [newest_commit, middle_commit, oldest_commit]}
   *   <li>Output: {@code [revision_1 (oldest), revision_2 (middle), revision_3 (newest)]}
   *   <li>The last project history item (oldest commit) becomes the first revision
   * </ul>
   *
   * @param projectHistory list of ontology commit changes (newest to oldest)
   * @return list of revisions (oldest to newest with sequential revision numbers)
   * @throws NullPointerException if projectHistory is null
   */
  @Nonnull
  public List<Revision> convertProjectHistoryToRevisions(
      @Nonnull List<OntologyCommitChange> projectHistory) {
    Objects.requireNonNull(projectHistory, "projectHistory cannot be null");

    var reversedHistory = new ArrayList<>(projectHistory);
    Collections.reverse(reversedHistory);
    return reversedHistory.stream()
        .map(changeCommitToRevisionConverter::convert)
        .collect(ImmutableList.toImmutableList());
  }
}
