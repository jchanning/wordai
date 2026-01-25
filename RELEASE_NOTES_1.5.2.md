# WordAI Release 1.5.2

**Release Date:** January 25, 2026  
**Version:** 1.5.2  
**Target Platform:** Oracle Cloud (Java 17), Local Development (Java 21)

## Overview
WordAI 1.5.2 includes critical bug fixes for mobile UI interactions, frontend stability improvements, and internal code cleanup. This release focuses on enhancing user experience on mobile devices and improving overall application reliability.

## What's New

### 🐛 Bug Fixes
- **Fixed Mobile Menu Navigation**: Resolved z-index layering issues preventing menu links from being clickable. Navigation elements now properly appear above overlay content.
- **Fixed Backspace Functionality**: Restored missing `deleteLastLetter()` function that was causing ReferenceError when users pressed backspace on mobile.
- **Fixed Mobile Header Layout**: Corrected flex-wrap behavior to prevent header wrapping on smaller screens, ensuring consistent layout across devices.

### 🔄 Improvements
- **Browser Cache Busting**: Added version query parameters to static assets (CSS and JavaScript) to ensure users receive latest code updates without manual cache clearing.
- **Frontend Testing**: Created test fixtures (`frontend-tests.html`, `nav-test.html`) for validating JavaScript logic and UI interactions.
- **Code Cleanup**: Removed deprecated selection strategy classes and consolidated codebase:
  - Removed: `MinimiseColumnLengths`, `SelectBellmanOptimal`, `SelectFixedFirstWord`, `SelectMaximumDictionaryReduction`, `SelectMostCommonLetters`, `Vowels`
  - Streamlined: `SelectionAlgo` interface and related implementations

### 📋 Technical Changes
- Updated `WordEntropy.java` with improved entropy calculations
- Enhanced `WordGamePlayer.java` for better game orchestration
- Improved `Dictionary.java` core word handling
- Refined `WordGameService.java` and `GameSession.java` models
- Updated deployment configuration for Oracle Cloud

## Deployment
- Successfully deployed to Oracle Cloud at `130.162.184.150:8080`
- Service running under Spring Boot production profile
- All dictionaries (4, 5, 6, 7-letter words) properly configured

## System Requirements
- **Java:** 17 or 21 (profile-dependent)
- **Spring Boot:** 3.4.0
- **Database:** H2 (embedded)

## Getting Started
1. Download the JAR: `wordai-1.5.2.jar`
2. Run with production profile:
   ```bash
   java -Xmx4096m -Dspring.profiles.active=prod -jar wordai-1.5.2.jar
   ```
3. Access the application at `http://localhost:8080`

## Known Issues
None reported.

## Future Roadmap
- Enhanced analytics and reporting
- Additional bot selection strategies
- UI/UX refinements based on user feedback

## Contributors
- FistralTech Development Team

## Support
For issues, feature requests, or questions, please refer to the [GitHub repository](https://github.com/jchanning/wordai).

---

**v1.5.2** is a stable release recommended for production deployment.
